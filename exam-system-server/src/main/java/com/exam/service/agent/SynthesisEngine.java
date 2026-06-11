package com.exam.service.agent;

import com.exam.service.agent.model.PlanReport;
import com.exam.service.agent.model.ResourceReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 多报告聚合引擎
 * 把 PlanAgent 的路径节点 × GenAgent 的资源列表 = 完整学习包
 */
@Slf4j
@Component
public class SynthesisEngine {

    /**
     * 将路径报告和资源报告合并为前端友好的结构
     */
    public Map<String, Object> synthesize(PlanReport plan, List<ResourceReport> resources) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 按路径节点 ID 分组资源
        Map<Long, List<ResourceReport>> resourcesByNode = resources.stream()
                .filter(r -> r.getPathNodeId() != null)
                .collect(Collectors.groupingBy(ResourceReport::getPathNodeId));

        // 去重：同一知识点同类型资源只保留最新
        Map<String, ResourceReport> deduped = new LinkedHashMap<>();
        for (ResourceReport r : resources) {
            String key = r.getPathNodeId() + "_" + r.getType();
            deduped.put(key, r);
        }

        List<Map<String, Object>> nodes = new ArrayList<>();
        if (plan != null && plan.getNodes() != null) {
            for (PlanReport.PlanNode pn : plan.getNodes()) {
                Map<String, Object> nodeMap = new LinkedHashMap<>();
                nodeMap.put("order", pn.getOrder());
                nodeMap.put("title", pn.getTitle());
                nodeMap.put("type", pn.getType());
                nodeMap.put("estimatedMinutes", pn.getEstimatedMinutes());
                nodeMap.put("reason", pn.getReason());
                nodeMap.put("knowledgePointIds", pn.getKnowledgePointIds());
                nodeMap.put("resources", new ArrayList<>(deduped.values()));
                nodes.add(nodeMap);
            }
        }

        result.put("courseName", plan != null ? plan.getCourseName() : "");
        result.put("nodes", nodes);
        result.put("totalResources", deduped.size());
        result.put("totalNodes", nodes.size());

        return result;
    }
}
