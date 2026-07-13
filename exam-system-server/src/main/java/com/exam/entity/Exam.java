package com.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("exams")
@Schema(description = "试卷")
public class Exam {

    @Schema(description = "试卷ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "试卷名称")
    private String name;

    @Schema(description = "试卷描述")
    private String description;

    @Schema(description = "考试时长（分钟）")
    private Integer duration;

    @Schema(description = "及格分数")
    private Integer passScore;

    @Schema(description = "总分")
    private Integer totalScore;

    @Schema(description = "题目数量")
    private Integer questionCount;

    @Schema(description = "状态：DRAFT/PUBLISHED/CLOSED")
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("create_time")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("update_time")
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private List<ExamQuestion> questions;

    @TableField(exist = false)
    private String statusText;

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_CLOSED = "CLOSED";
}
