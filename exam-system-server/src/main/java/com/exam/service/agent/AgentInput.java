package com.exam.service.agent;

import java.util.List;
import java.util.Map;

public class AgentInput {
    private final Long userId;
    private final Long sessionId;
    private final String userMessage;
    private final List<Map<String, String>> history;

    public AgentInput(Long userId, Long sessionId, String userMessage,
                      List<Map<String, String>> history) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.userMessage = userMessage;
        this.history = history;
    }

    public Long getUserId() { return userId; }
    public Long getSessionId() { return sessionId; }
    public String getUserMessage() { return userMessage; }
    public List<Map<String, String>> getHistory() { return history; }
}
