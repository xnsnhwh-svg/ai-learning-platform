package com.exam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.entity.Exam;
import com.exam.entity.ExamQuestion;
import com.exam.entity.ExamRecord;
import com.exam.entity.Question;
import com.exam.entity.QuestionAnswer;
import com.exam.entity.QuestionChoice;
import com.exam.mapper.ExamMapper;
import com.exam.mapper.ExamQuestionMapper;
import com.exam.mapper.ExamRecordMapper;
import com.exam.mapper.QuestionMapper;
import com.exam.mapper.QuestionChoiceMapper;
import com.exam.service.ExamService;
import com.exam.utils.IpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExamServiceImpl implements ExamService {

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private ExamQuestionMapper examQuestionMapper;

    @Autowired
    private ExamRecordMapper examRecordMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private QuestionChoiceMapper questionChoiceMapper;

    @Override
    public IPage<Exam> getExams(Integer page, Integer size, String keyword, String status) {
        Page<Exam> pageObj = new Page<>(page, size);
        IPage<Exam> result = examMapper.searchExams(pageObj, keyword, status);
        result.getRecords().forEach(this::formatExam);
        return result;
    }

    @Override
    public Exam getExamDetail(Long id) {
        Exam exam = examMapper.selectById(id);
        if (exam == null) throw new RuntimeException("试卷不存在");
        formatExam(exam);
        return exam;
    }

    @Override
    @Transactional
    public Exam createExam(Exam exam) {
        exam.setStatus(Exam.STATUS_DRAFT);
        exam.setCreatedAt(LocalDateTime.now());
        examMapper.insert(exam);
        return exam;
    }

    @Override
    @Transactional
    public Exam updateExam(Exam exam) {
        Exam existing = examMapper.selectById(exam.getId());
        if (existing == null) throw new RuntimeException("试卷不存在");
        if (!Exam.STATUS_DRAFT.equals(existing.getStatus())) throw new RuntimeException("只能编辑草稿状态的试卷");
        exam.setUpdatedAt(LocalDateTime.now());
        examMapper.updateById(exam);
        return exam;
    }

    @Override
    @Transactional
    public void deleteExam(Long id) {
        Exam exam = examMapper.selectById(id);
        if (exam == null) throw new RuntimeException("试卷不存在");
        examQuestionMapper.delete(new LambdaQueryWrapper<ExamQuestion>().eq(ExamQuestion::getExamId, id));
        examRecordMapper.delete(new LambdaQueryWrapper<ExamRecord>().eq(ExamRecord::getExamId, id));
        examMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void publishExam(Long id) {
        Exam exam = examMapper.selectById(id);
        if (exam == null) throw new RuntimeException("试卷不存在");
        int count = examQuestionMapper.countByExamId(id);
        if (count == 0) throw new RuntimeException("试卷没有题目，无法发布");
        exam.setStatus(Exam.STATUS_PUBLISHED);
        exam.setUpdatedAt(LocalDateTime.now());
        examMapper.updateById(exam);
    }

    @Override
    @Transactional
    public void closeExam(Long id) {
        Exam exam = examMapper.selectById(id);
        if (exam == null) throw new RuntimeException("试卷不存在");
        exam.setStatus(Exam.STATUS_CLOSED);
        exam.setUpdatedAt(LocalDateTime.now());
        examMapper.updateById(exam);
    }

    @Override
    public List<Map<String, Object>> getExamQuestions(Long examId) {
        List<ExamQuestion> eqs = examQuestionMapper.getQuestionsByExamId(examId);
        return eqs.stream().map(eq -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", eq.getId());
            map.put("questionOrder", eq.getQuestionOrder());
            map.put("questionScore", eq.getQuestionScore());
            if (eq.getQuestion() != null) {
                Question q = eq.getQuestion();
                map.put("questionId", q.getId());
                map.put("content", q.getTitle());
                map.put("type", q.getType());
                if ("CHOICE".equals(q.getType())) {
                    List<QuestionChoice> choices = questionChoiceMapper.selectList(
                            new LambdaQueryWrapper<QuestionChoice>().eq(QuestionChoice::getQuestionId, q.getId()));
                    map.put("choices", choices);
                }
            }
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ExamRecord startExam(Long examId, String studentName, String studentNumber, HttpServletRequest request) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) throw new RuntimeException("试卷不存在");
        if (!Exam.STATUS_PUBLISHED.equals(exam.getStatus())) throw new RuntimeException("试卷未发布");

        ExamRecord record = new ExamRecord();
        record.setExamId(examId);
        record.setStudentName(studentName);
        record.setStudentNumber(studentNumber);
        record.setTotalScore(0);
        record.setStartTime(LocalDateTime.now());
        record.setStatus(ExamRecord.STATUS_ONGOING);
        record.setWindowSwitches(0);
        if (request != null) record.setSubmitIp(IpUtils.getClientIp(request));
        record.setCreatedAt(LocalDateTime.now());
        examRecordMapper.insert(record);
        return record;
    }

    @Override
    @Transactional
    public ExamRecord submitExam(Long recordId, String answers, HttpServletRequest request) {
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null) throw new RuntimeException("考试记录不存在");
        if (!ExamRecord.STATUS_ONGOING.equals(record.getStatus())) throw new RuntimeException("考试已结束");

        record.setAnswers(answers);
        record.setEndTime(LocalDateTime.now());
        record.setStatus(ExamRecord.STATUS_FINISHED);
        if (request != null) record.setSubmitIp(IpUtils.getClientIp(request));
        record.setUpdatedAt(LocalDateTime.now());

        int totalScore = calculateScore(recordId, answers);
        record.setTotalScore(totalScore);

        examRecordMapper.updateById(record);
        return record;
    }

    private int calculateScore(Long recordId, String answersJson) {
        try {
            ExamRecord record = examRecordMapper.selectById(recordId);
            if (record == null) return 0;
            Exam exam = examMapper.selectById(record.getExamId());
            if (exam == null) return 0;

            List<ExamQuestion> eqs = examQuestionMapper.getQuestionsByExamId(record.getExamId());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> answerList = com.fasterxml.jackson.databind.json.JsonMapper.builder()
                    .build().readValue(answersJson, List.class);

            int total = 0;
            for (Map<String, Object> ans : answerList) {
                Object questionIdObj = ans.get("questionId");
                Object selectedObj = ans.get("selected");
                if (questionIdObj == null || selectedObj == null) continue;
                Long questionId = Long.valueOf(questionIdObj.toString());

                Optional<ExamQuestion> eqOpt = eqs.stream()
                        .filter(eq -> eq.getQuestionId() != null && eq.getQuestionId().equals(questionId))
                        .findFirst();
                if (eqOpt.isEmpty()) continue;

                Question question = questionMapper.selectById(questionId);
                if (question == null) continue;

                boolean correct = false;
                if ("CHOICE".equals(question.getType())) {
                    QuestionChoice choice = questionChoiceMapper.selectOne(
                            new LambdaQueryWrapper<QuestionChoice>()
                                    .eq(QuestionChoice::getQuestionId, questionId)
                                    .eq(QuestionChoice::getIsCorrect, true));
                    if (choice != null) {
                        correct = String.valueOf(choice.getSort()).equals(String.valueOf(selectedObj));
                    }
                } else if ("JUDGE".equals(question.getType())) {
                    QuestionAnswer qa = questionMapper.selectAnswerByQuestionId(questionId);
                    if (qa != null) {
                        correct = qa.getAnswer().trim().equalsIgnoreCase(String.valueOf(selectedObj));
                    }
                }

                if (correct) {
                    total += eqOpt.get().getQuestionScore();
                }
            }
            return total;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public ExamRecord getExamRecord(Long recordId) {
        ExamRecord record = examRecordMapper.selectById(recordId);
        if (record == null) throw new RuntimeException("考试记录不存在");
        Exam exam = examMapper.selectById(record.getExamId());
        if (exam != null) record.setExamName(exam.getName());
        formatRecord(record);
        return record;
    }

    @Override
    public List<ExamRecord> getStudentRecords(String studentNumber) {
        List<ExamRecord> records = examRecordMapper.getRecordsByStudent(studentNumber);
        records.forEach(this::formatRecord);
        return records;
    }

    @Override
    public IPage<ExamRecord> searchRecords(Integer page, Integer size, Long examId, String keyword) {
        Page<ExamRecord> pageObj = new Page<>(page, size);
        IPage<ExamRecord> result = examRecordMapper.searchRecords(pageObj, examId, keyword);
        result.getRecords().forEach(this::formatRecord);
        return result;
    }

    @Override
    public Map<String, Object> getExamStatistics(Long examId) {
        return examRecordMapper.getExamStatistics(examId);
    }

    private void formatExam(Exam exam) {
        switch (exam.getStatus()) {
            case "DRAFT": exam.setStatusText("草稿"); break;
            case "PUBLISHED": exam.setStatusText("已发布"); break;
            case "CLOSED": exam.setStatusText("已关闭"); break;
            default: exam.setStatusText("未知");
        }
    }

    private void formatRecord(ExamRecord record) {
        switch (record.getStatus()) {
            case "ONGOING": record.setStatusText("进行中"); break;
            case "FINISHED": record.setStatusText("已完成"); break;
            case "TIMEOUT": record.setStatusText("已超时"); break;
            default: record.setStatusText("未知");
        }
    }
}
