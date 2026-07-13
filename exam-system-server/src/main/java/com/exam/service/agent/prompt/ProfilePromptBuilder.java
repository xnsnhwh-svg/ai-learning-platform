package com.exam.service.agent.prompt;

import com.exam.service.agent.model.ProfileContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ProfilePromptBuilder implements PromptBuilder {

    @Override
    public String getPhase() { return "profiling"; }

    @Override
    public List<Map<String, String>> build(String userMessage, List<Map<String, String>> history) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content",
                "你是一个帮助学生了解自己学习水平的智能导师。" +
                "通过对话了解学生的以下维度：\n" +
                "1. 学习目标（想学什么）\n" +
                "2. 当前水平（零基础/入门/进阶）\n" +
                "3. 学习风格（理论/实践）\n" +
                "4. 可用时间（每周几小时）\n\n" +
                "一次只问1-2个问题。当收集到足够信息时，在末尾添加 [PROFILE_COMPLETE]。\n" +
                "并按以下格式输出画像JSON：\n" +
                "```json\n{\"goal\":\"...\",\"level\":\"...\",\"style\":\"...\",\"hoursPerWeek\":...}\n```\n\n" +
                "回复要友好、鼓励，使用中文。"));
        if (history != null) messages.addAll(history);
        messages.add(Map.of("role", "user", "content", userMessage));
        return messages;
    }

    public List<Map<String, String>> buildWithExistingProfile(String userMessage,
                                                               List<Map<String, String>> history) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content",
                "你是一个帮助学生了解自己学习情况的智能导师。" +
                "用户已有画像数据。如果用户想继续学习或明确表达了目标，直接回复 [PROFILE_COMPLETE]。\n\n" +
                "回复要友好、鼓励，使用中文。"));
        if (history != null) messages.addAll(history);
        messages.add(Map.of("role", "user", "content", userMessage));
        return messages;
    }
}
