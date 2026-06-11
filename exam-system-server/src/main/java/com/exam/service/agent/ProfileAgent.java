package com.exam.service.agent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.exam.service.LlmClient;
import com.exam.service.agent.model.AgentContext;
import com.exam.service.agent.model.AgentInput;
import com.exam.service.agent.model.AgentOutput;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 画像构建 Agent
 * 通过对话了解学生水平，输出结构化画像
 */
@Slf4j
@Component
public class ProfileAgent implements LearningAgent {

    @Autowired
    private LlmClient llmClient;

    private static final String SYSTEM_PROMPT =
            "你是一个学习画像构建助手。通过对话了解用户的学习水平和偏好。\n" +
            "规则（必须严格遵守）：\n" +
            "1. 每次只问一个问题\n" +
            "2. 总共只问恰好3个问题，不能多也不能少\n" +
            "3. 前2个问题返回状态profiling，第3个问题用户回答后，第4次调用时必须返回status:complete\n" +
            "4. 需要了解：编程基础、学习目标、每周学习时长\n" +
            "还在提问时返回：{\"status\":\"profiling\",\"type\":\"question\",\"text\":\"问题\",\"options\":[\"选项1\",\"选项2\",\"选项3\",\"选项4\"]}\n" +
            "画像完成时（第3个问题回答后）必须返回：{\"status\":\"complete\",\"profile\":{\"level\":\"初级|中级|高级\",\"knowledge_map\":{\"java_basics\":{\"level\":0.5,\"label\":\"Java基础\"}},\"cognitive_style\":{\"type\":\"depth_first\",\"avg_session_min\":30},\"weak_points\":[{\"topic\":\"薄弱点\",\"level\":0.3}],\"learning_goal\":{\"target\":\"学习目标\"}}}";

    @Override
    public String getRole() {
        return "ProfileAgent";
    }

    @Override
    public String buildSystemPrompt(AgentContext ctx) {
        return SYSTEM_PROMPT;
    }

    @Override
    public AgentOutput execute(AgentInput input) {
        AgentContext ctx = input.getContext();
        List<Map<String, String>> history = ctx.getConversationHistory();

        String response = llmClient.chat(buildSystemPrompt(ctx), history);
        String jsonStr = extractJson(response);

        AgentOutput output = new AgentOutput();
        output.setAgentRole(getRole());
        output.setRawResponse(response);

        try {
            JSONObject parsed = JSON.parseObject(jsonStr);
            String status = parsed.getString("status");
            if ("complete".equals(status)) {
                output.setStatus("success");
                output.setStructuredData(parsed.get("profile"));
            } else {
                output.setStatus("partial");
                output.setStructuredData(parsed);
            }
        } catch (Exception e) {
            log.error("ProfileAgent 解析响应失败", e);
            output.setStatus("failed");
            output.setStructuredData(Map.of("error", "解析失败"));
        }

        return output;
    }

    private String extractJson(String text) {
        int s = text.indexOf("```json"), e = text.lastIndexOf("```");
        if (s != -1 && e > s) return text.substring(s + 7, e).trim();
        s = text.indexOf("{"); e = text.lastIndexOf("}");
        if (s != -1 && e > s) return text.substring(s, e + 1).trim();
        return text;
    }
}
