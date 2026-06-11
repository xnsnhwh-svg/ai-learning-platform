package com.exam.service.agent.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Agent 上下文对象
 * 每个 Agent 收到两份上下文：globalContext（全局共享）+ localContext（自己负责的范围）
 */
@Data
public class AgentContext {
    private String sessionId;
    private String userId;
    private Map<String, Object> globalContext;
    private Map<String, Object> localContext;
    private List<Map<String, String>> conversationHistory;
}
