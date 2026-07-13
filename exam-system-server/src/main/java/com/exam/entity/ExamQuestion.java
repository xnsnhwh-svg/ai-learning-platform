package com.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("exam_questions")
@Schema(description = "试卷题目关联")
public class ExamQuestion {

    @Schema(description = "关联ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "试卷ID")
    private Long examId;

    @Schema(description = "题目ID")
    private Long questionId;

    @Schema(description = "题目分值")
    private Integer questionScore;

    @Schema(description = "题目顺序")
    private Integer questionOrder;

    @TableField(exist = false)
    private Question question;
}
