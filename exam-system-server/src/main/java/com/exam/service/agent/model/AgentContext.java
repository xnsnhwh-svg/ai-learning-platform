package com.exam.service.agent.model;

import java.util.List;
import java.util.Map;

/**
 * Agent涓婁笅鏂囦俊鎭? */
public class AgentContext {

    private Long sessionId;
    private Long userId;
    private GlobalContext globalContext;
    private LocalContext localContext;
    private List<Map<String, String>> conversationHistory;

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public GlobalContext getGlobalContext() {
        return globalContext;
    }

    public void setGlobalContext(GlobalContext globalContext) {
        this.globalContext = globalContext;
    }

    public LocalContext getLocalContext() {
        return localContext;
    }

    public void setLocalContext(LocalContext localContext) {
        this.localContext = localContext;
    }

    public List<Map<String, String>> getConversationHistory() {
        return conversationHistory;
    }

    public void setConversationHistory(List<Map<String, String>> conversationHistory) {
        this.conversationHistory = conversationHistory;
    }

    /**
     * 鍏ㄥ眬涓婁笅鏂?- 璺ˋgent鍏变韩鐨勪俊鎭?     */
    public static class GlobalContext {
        private String userProfileSummary;
        private String knowledgeDomain;
        private String sessionGoal;

        public String getUserProfileSummary() {
            return userProfileSummary;
        }

        public void setUserProfileSummary(String userProfileSummary) {
            this.userProfileSummary = userProfileSummary;
        }

        public String getKnowledgeDomain() {
            return knowledgeDomain;
        }

        public void setKnowledgeDomain(String knowledgeDomain) {
            this.knowledgeDomain = knowledgeDomain;
        }

        public String getSessionGoal() {
            return sessionGoal;
        }

        public void setSessionGoal(String sessionGoal) {
            this.sessionGoal = sessionGoal;
        }
    }

    /**
     * 灞€閮ㄤ笂涓嬫枃 - 褰撳墠Agent鐗瑰畾鐨勪俊鎭?     */
    public static class LocalContext {
        private String specificInstruction;
        private List<String> scope;
        private String mode;

        public String getSpecificInstruction() {
            return specificInstruction;
        }

        public void setSpecificInstruction(String specificInstruction) {
            this.specificInstruction = specificInstruction;
        }

        public List<String> getScope() {
            return scope;
        }

        public void setScope(List<String> scope) {
            this.scope = scope;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }
    }
}
