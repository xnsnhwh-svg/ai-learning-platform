package com.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.entity.ExamQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ExamQuestionMapper extends BaseMapper<ExamQuestion> {

    @Select("SELECT eq.*, q.* FROM exam_questions eq " +
            "LEFT JOIN questions q ON eq.question_id = q.id " +
            "WHERE eq.exam_id = #{examId} ORDER BY eq.question_order")
    List<ExamQuestion> getQuestionsByExamId(Long examId);

    @Select("SELECT COUNT(*) FROM exam_questions WHERE exam_id = #{examId}")
    int countByExamId(Long examId);
}
