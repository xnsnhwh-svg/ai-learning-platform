package com.exam.service.agent.model;

import lombok.Data;

/**
 * 资源报告
 */
@Data
public class ResourceReport {
    private Long pathNodeId;
    private Long knowledgePointId;
    private String type;        // document | quiz | mindmap
    private String title;
    private String difficulty;  // L1 | L2 | L3
    private Object content;
}
