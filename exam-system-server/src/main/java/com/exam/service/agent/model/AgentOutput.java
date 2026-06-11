package com.exam.service.agent.model;

import lombok.Data;

/**
 * Agent 输出
 */
@Data
public class AgentOutput {
    private String agentRole;
    private String status;          // success | partial | failed
    private Object structuredData;  // Agent 产出的具体数据
    private String rawResponse;     // 原始大模型返回（调试用）
    private int tokensUsed;
}
