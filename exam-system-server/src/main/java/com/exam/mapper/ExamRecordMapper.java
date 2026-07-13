package com.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.entity.ExamRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface ExamRecordMapper extends BaseMapper<ExamRecord> {

    @Select("SELECT er.*, e.name as exam_name FROM exam_records er " +
            "LEFT JOIN exams e ON er.exam_id = e.id " +
            "WHERE er.student_number = #{studentNumber} " +
            "ORDER BY er.create_time DESC")
    List<ExamRecord> getRecordsByStudent(@Param("studentNumber") String studentNumber);

    @Select("<script>" +
            "SELECT er.*, e.name as exam_name FROM exam_records er " +
            "LEFT JOIN exams e ON er.exam_id = e.id WHERE 1=1 " +
            "<if test='examId != null'> AND er.exam_id = #{examId} </if>" +
            "<if test='keyword != null and keyword != \"\"'> " +
            "AND (er.student_name LIKE CONCAT('%', #{keyword}, '%') " +
            "OR er.student_number LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "ORDER BY er.create_time DESC" +
            "</script>")
    IPage<ExamRecord> searchRecords(Page<?> page,
                                   @Param("examId") Long examId,
                                   @Param("keyword") String keyword);

    @Select("SELECT COUNT(*), SUM(total_score), AVG(total_score) " +
            "FROM exam_records WHERE exam_id = #{examId} AND status = 'FINISHED'")
    Map<String, Object> getExamStatistics(@Param("examId") Long examId);

    @Update("UPDATE exam_records SET status = 'TIMEOUT', end_time = NOW() " +
            "WHERE status = 'ONGOING' AND start_time &lt; DATE_SUB(NOW(), INTERVAL duration MINUTE)")
    int timeoutOngoingRecords();
}
