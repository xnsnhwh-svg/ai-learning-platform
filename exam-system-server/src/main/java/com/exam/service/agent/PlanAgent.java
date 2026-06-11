package com.exam.service.agent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
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
 * 路径规划 Agent
 * 基于画像 + 知识图谱，生成个性化学习路径
 */
@Slf4j
@Component
public class PlanAgent implements LearningAgent {

    @Autowired
    private LlmClient llmClient;

    private static final String SYSTEM_PROMPT =
            "你是学习路径规划助手。根据用户画像和知识图谱生成学习路径。\n" +
            "规则：\n" +
            "1. 必须遵守知识图谱的前置依赖（没学前置知识点就不能先学后续）\n" +
            "2. 薄弱点优先安排，但需穿插巩固已有内容（间隔重复）\n" +
            "3. 每节点预估学习时长匹配用户的 avg_session_min\n" +
            "4. 路径节点 10-25 个，每个标注学习类型（review/new_learn/reinforce）\n" +
            "返回格式：{\"title\":\"路径标题\",\"courseId\":1,\"nodes\":[{\"order\":1,\"title\":\"节点标题\",\"type\":\"review|new_learn|reinforce\",\"estimatedMinutes\":30,\"reason\":\"原因\",\"knowledgePointIds\":[1,2]}]}";

    @Override
    public String getRole() {
        return "PlanAgent";
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
            PlanReport report = new PlanReport();
            report.setCourseName(parsed.getString("title"));
            report.setCourseId(parsed.getLong("courseId"));

            List<PlanReport.PlanNode> nodes = new ArrayList<>();
            JSONArray nodesArray = parsed.getJSONArray("nodes");
            if (nodesArray != null) {
                for (int i = 0; i < nodesArray.size(); i++) {
                    JSONObject nj = nodesArray.getJSONObject(i);
                    PlanReport.PlanNode node = new PlanReport.PlanNode();
                    node.setOrder(nj.getIntValue("order"));
                    node.setTitle(nj.getString("title"));
                    node.setType(nj.getString("type"));
                    node.setEstimatedMinutes(nj.getIntValue("estimatedMinutes"));
                    node.setReason(nj.getString("reason"));
                    if (nj.containsKey("knowledgePointIds")) {
                        node.setKnowledgePointIds(nj.getJSONArray("knowledgePointIds").toJavaList(Long.class));
                    }
                    nodes.add(node);
                }
            }
            report.setNodes(nodes);

            output.setStatus("success");
            output.setStructuredData(report);
        } catch (Exception e) {
            log.error("PlanAgent 解析响应失败", e);
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
