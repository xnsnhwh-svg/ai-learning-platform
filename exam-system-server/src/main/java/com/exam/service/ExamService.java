package com.exam.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.exam.entity.Exam;
import com.exam.entity.ExamRecord;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

public interface ExamService {

    IPage<Exam> getExams(Integer page, Integer size, String keyword, String status);

    Exam getExamDetail(Long id);

    Exam createExam(Exam exam);

    Exam updateExam(Exam exam);

    void deleteExam(Long id);

    void publishExam(Long id);

    void closeExam(Long id);

    List<Map<String, Object>> getExamQuestions(Long examId);

    ExamRecord startExam(Long examId, String studentName, String studentNumber, HttpServletRequest request);

    ExamRecord submitExam(Long recordId, String answers, HttpServletRequest request);

    ExamRecord getExamRecord(Long recordId);

    List<ExamRecord> getStudentRecords(String studentNumber);

    IPage<ExamRecord> searchRecords(Integer page, Integer size, Long examId, String keyword);

    Map<String, Object> getExamStatistics(Long examId);
}
