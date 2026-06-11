package com.exam.service.agent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.exam.service.LlmClient;
import com.exam.service.agent.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 资源生成 Agent
 * 根据知识点 + 资源类型，生成具体学习内容（文档/题目/思维导图）
 */
@Slf4j
@Component
public class GenAgent implements LearningAgent {

    @Autowired
    private LlmClient llmClient;

    private static final String SYSTEM_PROMPT =
            "你是学习资源生成助手。根据要求生成学习资料。\n" +
            "文档：{\"type\":\"document\",\"title\":\"标题\",\"content\":\"# 标题\\n\\nMarkdown内容\"}\n" +
            "练习题：{\"type\":\"quiz\",\"title\":\"标题\",\"content\":{\"questions\":[{\"content\":\"题目\",\"type\":\"CHOICE\",\"difficulty\":\"EASY\",\"options\":[{\"content\":\"A\",\"isCorrect\":true}],\"analysis\":\"解析\"}]}}\n" +
            "思维导图：{\"type\":\"mindmap\",\"title\":\"标题\",\"content\":{\"topic\":\"主题\",\"children\":[{\"topic\":\"子主题\"}]}}";

    @Override
    public String getRole() {
        return "GenAgent";
    }

    @Override
    public String buildSystemPrompt(AgentContext ctx) {
        return SYSTEM_PROMPT;
    }

    @Override
    public AgentOutput execute(AgentInput input) {
        AgentContext ctx = input.getContext();
        String userPrompt = input.getUserMessage();

        String response = llmClient.chat(buildSystemPrompt(ctx),
                List.of(Map.of("role", "user", "content", userPrompt)));
        String jsonStr = extractJson(response);

        AgentOutput output = new AgentOutput();
        output.setAgentRole(getRole());
        output.setRawResponse(response);

        try {
            JSONObject parsed = JSON.parseObject(jsonStr);
            ResourceReport report = new ResourceReport();
            report.setType(parsed.getString("type"));
            report.setTitle(parsed.getString("title"));
            report.setContent(parsed.get("content"));

            output.setStatus("success");
            output.setStructuredData(report);
        } catch (Exception e) {
            log.error("GenAgent 解析响应失败", e);
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
