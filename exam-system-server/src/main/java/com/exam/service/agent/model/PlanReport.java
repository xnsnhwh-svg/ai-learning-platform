package com.exam.service.agent.model;

import lombok.Data;

import java.util.List;

/**
 * 路径报告
 */
@Data
public class PlanReport {
    private String courseName;
    private Long courseId;
    private List<PlanNode> nodes;

    @Data
    public static class PlanNode {
        private int order;
        private String title;
        private String type;            // review | new_learn | reinforce
        private int estimatedMinutes;
        private String reason;
        private List<Long> knowledgePointIds;
    }
}
