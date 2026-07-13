package com.exam.service.agent;

import com.exam.service.agent.model.ConflictResult;
import com.exam.service.agent.model.PlanReport;
import com.exam.service.agent.model.ResourceReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 鍐茬獊妫€娴嬪櫒
 * 璐熻矗妫€娴嬭矾寰勮鍒掑拰璧勬簮鐢熸垚涔嬮棿鐨勪笉涓€鑷? */
@Component
public class ConflictDetector {

    private static final Logger log = LoggerFactory.getLogger(ConflictDetector.class);

    /**
     * 妫€娴嬪啿绐?     * @param plan 璺緞瑙勫垝鎶ュ憡
     * @param resources 鐢熸垚鐨勮祫婧愬垪琛?     * @param userLevel 鐢ㄦ埛姘村钩 (L1/L2/L3)
     * @return 鍐茬獊妫€娴嬬粨鏋?     */
    public ConflictResult detect(PlanReport plan, List<ResourceReport> resources, String userLevel) {
        List<String> missingCoverage = detectMissingCoverage(plan, resources);
        List<String> difficultyMismatch = detectDifficultyMismatch(resources, userLevel);

        int score = missingCoverage.size() * 10 + difficultyMismatch.size() * 5;

        ConflictResult result = new ConflictResult(score, missingCoverage, difficultyMismatch);

        log.info("Conflict detection: score={}, missing={}, mismatched={}",
                score, missingCoverage.size(), difficultyMismatch.size());

        return result;
    }

    /**
     * 妫€娴嬭鐩栫己澶?     * PlanAgent瑙勫垝鐨勮矾寰勮妭鐐癸紝GenAgent鏄惁姣忎釜閮界敓鎴愪簡璧勬簮
     */
    private List<String> detectMissingCoverage(PlanReport plan, List<ResourceReport> resources) {
        if (plan == null || plan.getNodes() == null) {
            return List.of();
        }

        // 鑾峰彇鎵€鏈夎鍒掔殑鐭ヨ瘑鐐笽D
        Set<Long> plannedKpIds = plan.getAllKnowledgePointIds().stream()
                .collect(Collectors.toSet());

        // 鑾峰彇宸茬敓鎴愯祫婧愮殑鐭ヨ瘑鐐笽D
        Set<Long> generatedKpIds = resources.stream()
                .filter(r -> r.getKnowledgePointId() != null)
                .map(ResourceReport::getKnowledgePointId)
                .collect(Collectors.toSet());

        // 鎵惧嚭缂哄け鐨勭煡璇嗙偣
        List<String> missing = new ArrayList<>();
        for (Long kpId : plannedKpIds) {
            if (!generatedKpIds.contains(kpId)) {
                missing.add("KP_" + kpId);
            }
        }

        return missing;
    }

    /**
     * 妫€娴嬮毦搴︿笉鍖归厤
     * 鐢ㄦ埛姘村钩鏄疞1锛屼絾鐢熸垚鐨勮祫婧愭爣浜哃3
     */
    private List<String> detectDifficultyMismatch(List<ResourceReport> resources, String userLevel) {
        if (userLevel == null || resources == null) {
            return List.of();
        }

        int userLevelNum = parseLevel(userLevel);
        List<String> mismatched = new ArrayList<>();

        for (ResourceReport resource : resources) {
            if (resource.getDifficulty() != null) {
                int resourceLevel = parseLevel(resource.getDifficulty());
                // 濡傛灉璧勬簮闅惧害瓒呰繃鐢ㄦ埛姘村钩2绾т互涓婏紝璁や负涓嶅尮閰?                if (resourceLevel - userLevelNum >= 2) {
                    mismatched.add(resource.getTitle() + " (闅惧害:" + resource.getDifficulty() + ")");
                }
            }
        }

        return mismatched;
    }

    /**
     * 瑙ｆ瀽闅惧害绾у埆
     */
    private int parseLevel(String level) {
        if (level == null) return 1;
        return switch (level.toUpperCase()) {
            case "L1", "鍒濈骇", "EASY" -> 1;
            case "L2", "涓骇", "MEDIUM" -> 2;
            case "L3", "楂樼骇", "HARD" -> 3;
            default -> 1;
        };
    }

    /**
     * 鍒ゆ柇鏄惁闇€瑕侀噸鏂扮敓鎴?     */
    public boolean needRegenerate(ConflictResult result) {
        return result.isNeedRegenerate();
    }
}
