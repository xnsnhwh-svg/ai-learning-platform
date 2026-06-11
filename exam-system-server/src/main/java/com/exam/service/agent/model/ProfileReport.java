package com.exam.service.agent.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 画像报告
 */
@Data
public class ProfileReport {
    private String level;
    private Map<String, Map<String, Object>> knowledgeMap;
    private Map<String, Object> cognitiveStyle;
    private List<Map<String, Object>> weakPoints;
    private Map<String, Object> learningGoal;
}
