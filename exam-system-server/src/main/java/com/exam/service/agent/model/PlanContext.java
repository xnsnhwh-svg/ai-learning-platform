package com.exam.service.agent.model;

import java.util.List;
import java.util.Map;

public class PlanContext extends AgentContext {
    private final Long courseId;
    private final String courseInfo;
    private final String knowledgeInfo;

    public PlanContext(Long userId, Long sessionId, String userMessage,
                       List<Map<String, String>> conversationHistory,
                       Long courseId, String courseInfo, String knowledgeInfo) {
        super(userId, sessionId, userMessage, conversationHistory);
        this.courseId = courseId;
        this.courseInfo = courseInfo;
        this.knowledgeInfo = knowledgeInfo;
    }

    public Long getCourseId() { return courseId; }
    public String getCourseInfo() { return courseInfo; }
    public String getKnowledgeInfo() { return knowledgeInfo; }
}
