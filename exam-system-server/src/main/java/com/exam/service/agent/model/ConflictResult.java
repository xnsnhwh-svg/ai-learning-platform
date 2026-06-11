package com.exam.service.agent.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 冲突检测结果
 */
@Data
@AllArgsConstructor
public class ConflictResult {
    private int score;                      // 冲突分，>30 需要重新调度
    private List<String> missingPoints;     // 覆盖缺失的知识点 code
    private List<String> mismatchedPoints;  // 难度不匹配的知识点 code
    private boolean passed;                 // score <= 30 则通过
}
