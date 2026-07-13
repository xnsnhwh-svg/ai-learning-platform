package com.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 路径节点实体类
 * 路径节点是学习路径中的一个学习单元，包含学习类型、时长和关联的知识点
 */
@Data
@TableName("path_node")
@Schema(description = "路径节点")
public class PathNode {

    @Schema(description = "节点ID，唯一标识", example = "1")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "所属路径ID", example = "1")
    private Long pathId;

    @Schema(description = "节点序号", example = "1")
    @JsonAlias("order")
    private Integer nodeOrder;

    @Schema(description = "节点标题", example = "复习类与对象")
    private String title;

    @Schema(description = "学习类型：review-复习，new_learn-新学，reinforce-强化", example = "review")
    @JsonAlias("type")
    private String nodeType;

    @Schema(description = "预估学习时长（分钟）", example = "30")
    private Integer estimatedMinutes;

    @Schema(description = "安排此节点的原因（AI生成）", example = "这是面向对象的基础，需要先掌握")
    private String reason;

    @Schema(description = "学习目标（AI生成）", example = "掌握Python列表的创建、索引、切片和常用方法")
    private String learningObjectives;

    @Schema(description = "教学内容大纲（AI生成，分步骤）", example = "1. 列表的创建\n2. 索引访问\n3. 切片操作\n4. 常见方法")
    private String contentOutline;

    @Schema(description = "代码示例（AI生成）", example = "fruits = ['apple', 'banana', 'cherry']\nprint(fruits[0])")
    private String codeExample;

    @Schema(description = "练习任务（AI生成）", example = "创建一个包含10个学生成绩的列表，计算平均分")
    private String practiceTask;

    @Schema(description = "状态：pending-待开始，in_progress-进行中，completed-已完成", example = "pending")
    private String status;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // ========== 非数据库字段 ==========

    @Schema(description = "关联的知识点列表")
    @TableField(exist = false)
    private List<KnowledgePoint> knowledgePoints;

    @Schema(description = "关联的知识点ID列表")
    @TableField(exist = false)
    private List<Long> knowledgePointIds;

    @Schema(description = "该节点的生成资源列表")
    @TableField(exist = false)
    private List<GeneratedResource> resources;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPathId() { return pathId; }
    public void setPathId(Long pathId) { this.pathId = pathId; }
    public Integer getNodeOrder() { return nodeOrder; }
    public void setNodeOrder(Integer nodeOrder) { this.nodeOrder = nodeOrder; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getNodeType() { return nodeType; }
    public void setNodeType(String nodeType) { this.nodeType = nodeType; }
    public Integer getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(Integer estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
    public java.util.List<KnowledgePoint> getKnowledgePoints() { return knowledgePoints; }
    public void setKnowledgePoints(java.util.List<KnowledgePoint> knowledgePoints) { this.knowledgePoints = knowledgePoints; }
    public java.util.List<Long> getKnowledgePointIds() { return knowledgePointIds; }
    public void setKnowledgePointIds(java.util.List<Long> knowledgePointIds) { this.knowledgePointIds = knowledgePointIds; }

}