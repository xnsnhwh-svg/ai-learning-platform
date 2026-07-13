package com.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("exam_records")
@Schema(description = "考试记录")
public class ExamRecord {

    @Schema(description = "记录ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "试卷ID")
    private Long examId;

    @Schema(description = "考生姓名")
    private String studentName;

    @Schema(description = "考生学号/工号")
    private String studentNumber;

    @Schema(description = "总得分")
    private Integer totalScore;

    @Schema(description = "答题记录（JSON格式）")
    private String answers;

    @Schema(description = "开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @Schema(description = "状态：ONGOING/FINISHED/TIMEOUT")
    private String status;

    @Schema(description = "提交IP")
    private String submitIp;

    @Schema(description = "切屏次数")
    private Integer windowSwitches;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("create_time")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("update_time")
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String statusText;

    @TableField(exist = false)
    private String examName;

    public static final String STATUS_ONGOING = "ONGOING";
    public static final String STATUS_FINISHED = "FINISHED";
    public static final String STATUS_TIMEOUT = "TIMEOUT";
}
