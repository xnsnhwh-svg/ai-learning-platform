package com.exam.service.agent;

import com.exam.service.agent.model.ConflictResult;
import com.exam.service.agent.model.PlanReport;
import com.exam.service.agent.model.ProfileReport;
import com.exam.service.agent.model.ResourceReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 规则层冲突检测（纯规则，不调大模型）
 */
@Slf4j
@Component
public class ConflictDetector {

    private static final int THRESHOLD = 30;

    /**
     * 检测覆盖缺失和难度不匹配
     * @return ConflictResult，score > 30 表示需要重新调度
     */
    public ConflictResult detect(PlanReport plan, List<ResourceReport> resources, ProfileReport profile) {
        List<String> missing = detectCoverageGaps(plan, resources);
        List<String> mismatched = detectDifficultyMismatch(resources, profile);

        int score = missing.size() * 10 + mismatched.size() * 5;
        boolean passed = score <= THRESHOLD;

        if (!passed) {
            log.warn("冲突检测未通过: score={}, missing={}, mismatched={}", score, missing.size(), mismatched.size());
        }

        return new ConflictResult(score, missing, mismatched, passed);
    }

    /**
     * 检测 1：覆盖缺失
     * PlanAgent 规划的路径节点，GenAgent 是否每个都生成了资源
     */
    private List<String> detectCoverageGaps(PlanReport plan, List<ResourceReport> resources) {
        if (plan == null || plan.getNodes() == null) return Collections.emptyList();

        Set<String> plannedKps = plan.getNodes().stream()
                .filter(n -> n.getKnowledgePointIds() != null)
                .flatMap(n -> n.getKnowledgePointIds().stream())
                .map(String::valueOf)
                .collect(Collectors.toSet());

        Set<String> coveredKps = resources.stream()
                .filter(r -> r.getKnowledgePointId() != null)
                .map(r -> String.valueOf(r.getKnowledgePointId()))
                .collect(Collectors.toSet());

        plannedKps.removeAll(coveredKps);
        return new ArrayList<>(plannedKps);
    }

    /**
     * 检测 2：难度不匹配
     * 画像说用户是 L1 水平，生成的题是否标了 L3
     */
    private List<String> detectDifficultyMismatch(List<ResourceReport> resources, ProfileReport profile) {
        if (profile == null || resources == null) return Collections.emptyList();

        String userLevel = profile.getLevel();
        if (userLevel == null) return Collections.emptyList();

        int maxRecommended = switch (userLevel) {
            case "初级" -> 1;
            case "中级" -> 2;
            case "高级" -> 3;
            default -> 3;
        };

        return resources.stream()
                .filter(r -> {
                    if (r.getDifficulty() == null) return false;
                    int diff = switch (r.getDifficulty()) {
                        case "L1" -> 1;
                        case "L2" -> 2;
                        case "L3" -> 3;
                        default -> 1;
                    };
                    return diff > maxRecommended + 1; // 允许跨一级
                })
                .map(r -> (r.getKnowledgePointId() != null ? r.getKnowledgePointId() : "?") + ":" + r.getDifficulty())
                .collect(Collectors.toList());
    }
}
