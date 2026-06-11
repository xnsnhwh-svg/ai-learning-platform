package com.exam.service.agent.model;

import lombok.Data;

import java.util.List;

/**
 * 调度计划
 */
@Data
public class OrchestrationPlan {
    private String complexity;  // simple | complex
    private List<PlanStep> plan;

    @Data
    public static class PlanStep {
        private int step;
        private String agent;           // profile | plan | generate
        private String task;
        private List<Integer> dependsOn;
        private Object localContext;
        private boolean parallel;
    }
}
