package com.exam.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.exam.common.Result;
import com.exam.entity.Exam;
import com.exam.entity.ExamRecord;
import com.exam.service.ExamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exams")
@Tag(name = "考试管理", description = "试卷CRUD、发布、考试作答、成绩查询等")
public class ExamController {

    @Autowired
    private ExamService examService;

    @GetMapping
    @Operation(summary = "分页查询试卷列表")
    public Result<IPage<Exam>> getExams(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return Result.success(examService.getExams(page, size, keyword, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取试卷详情")
    public Result<Exam> getExamDetail(@PathVariable Long id) {
        return Result.success(examService.getExamDetail(id));
    }

    @PostMapping
    @Operation(summary = "创建试卷（草稿）")
    public Result<Exam> createExam(@RequestBody Exam exam) {
        return Result.success(examService.createExam(exam));
    }

    @PutMapping
    @Operation(summary = "更新试卷")
    public Result<Exam> updateExam(@RequestBody Exam exam) {
        return Result.success(examService.updateExam(exam));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除试卷")
    public Result<Void> deleteExam(@PathVariable Long id) {
        examService.deleteExam(id);
        return Result.success("删除成功");
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "发布试卷")
    public Result<Void> publishExam(@PathVariable Long id) {
        examService.publishExam(id);
        return Result.success("发布成功");
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "关闭试卷")
    public Result<Void> closeExam(@PathVariable Long id) {
        examService.closeExam(id);
        return Result.success("关闭成功");
    }

    @GetMapping("/{examId}/questions")
    @Operation(summary = "获取试卷题目列表")
    public Result<List<Map<String, Object>>> getExamQuestions(@PathVariable Long examId) {
        return Result.success(examService.getExamQuestions(examId));
    }

    @PostMapping("/{examId}/start")
    @Operation(summary = "开始考试")
    public Result<ExamRecord> startExam(
            @PathVariable Long examId,
            @RequestParam String studentName,
            @RequestParam String studentNumber,
            HttpServletRequest request) {
        return Result.success(examService.startExam(examId, studentName, studentNumber, request));
    }

    @PostMapping("/records/{recordId}/submit")
    @Operation(summary = "提交考试答案")
    public Result<ExamRecord> submitExam(
            @PathVariable Long recordId,
            @RequestBody String answers,
            HttpServletRequest request) {
        return Result.success(examService.submitExam(recordId, answers, request));
    }

    @GetMapping("/records/{recordId}")
    @Operation(summary = "获取考试记录详情")
    public Result<ExamRecord> getExamRecord(@PathVariable Long recordId) {
        return Result.success(examService.getExamRecord(recordId));
    }

    @GetMapping("/records/student/{studentNumber}")
    @Operation(summary = "获取学生考试记录列表")
    public Result<List<ExamRecord>> getStudentRecords(@PathVariable String studentNumber) {
        return Result.success(examService.getStudentRecords(studentNumber));
    }

    @GetMapping("/records")
    @Operation(summary = "分页查询考试记录（管理端）")
    public Result<IPage<ExamRecord>> searchRecords(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long examId,
            @RequestParam(required = false) String keyword) {
        return Result.success(examService.searchRecords(page, size, examId, keyword));
    }

    @GetMapping("/{examId}/statistics")
    @Operation(summary = "获取考试统计数据")
    public Result<Map<String, Object>> getExamStatistics(@PathVariable Long examId) {
        return Result.success(examService.getExamStatistics(examId));
    }
}
