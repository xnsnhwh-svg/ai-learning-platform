package com.exam.service.agent.model;

import lombok.Data;

/**
 * Agent 输入
 */
@Data
public class AgentInput {
    private AgentContext context;
    private String userMessage;
}
