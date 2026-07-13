package com.exam.service.agent.model;

import com.exam.entity.PathNode;

import java.util.List;
import java.util.Map;

public class GenContext extends AgentContext {
    private final Long courseId;
    private final Long pathId;
    private final List<PathNode> pathNodes;

    public GenContext(Long userId, Long sessionId, String userMessage,
                      List<Map<String, String>> conversationHistory, Long courseId,
                      Long pathId, List<PathNode> pathNodes) {
        super(userId, sessionId, userMessage, conversationHistory);
        this.courseId = courseId;
        this.pathId = pathId;
        this.pathNodes = pathNodes;
    }

    public Long getCourseId() { return courseId; }
    public Long getPathId() { return pathId; }
    public List<PathNode> getPathNodes() { return pathNodes; }
}
