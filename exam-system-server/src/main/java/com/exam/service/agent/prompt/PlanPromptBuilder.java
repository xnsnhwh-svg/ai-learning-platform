package com.exam.service.agent.prompt;

import com.exam.service.agent.model.PlanContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class PlanPromptBuilder implements PromptBuilder {

    @Override
    public String getPhase() { return "planning"; }

    public List<Map<String, String>> build(PlanContext context) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", buildSystemPrompt(context)));
        if (context.getConversationHistory() != null) {
            messages.addAll(context.getConversationHistory());
        }
        messages.add(Map.of("role", "user", "content", context.getUserMessage()));
        return messages;
    }

    @Override
    public List<Map<String, String>> build(String userMessage, List<Map<String, String>> history) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content",
                "你是一个学习路径规划专家。根据用户需求规划个性化学习路径。\n\n" +
                "按以下格式输出：\n" +
                "[PLAN]\n" +
                "节点标题|new_learn|30|安排原因|知识点ID\n" +
                "[/PLAN]"));
        if (history != null) messages.addAll(history);
        messages.add(Map.of("role", "user", "content", userMessage));
        return messages;
    }

    private String buildSystemPrompt(PlanContext context) {
        return "你是一个专业的学习路径规划专家与课程设计师。你的任务是：基于学生画像和课程知识图谱，规划**有教学深度**的个性化学习路径。\n\n" +
                "当前可用的课程：\n" + context.getCourseInfo() + "\n\n" +
                (context.getKnowledgeInfo().isEmpty() ? "" :
                 "课程知识点（含依赖关系）：\n" + context.getKnowledgeInfo() + "\n\n") +
                "每个学习节点必须包含完整的教学设计，不能只是一个标题。\n\n" +
                "输出格式：先写一段分析，然后输出以下格式（字段用 | 分隔）：\n" +
                "[PLAN]\n" +
                "节点标题|new_learn|30|安排原因|知识点ID|学习目标|教学内容大纲(分步骤,用###分隔)|代码示例(用```包裹)|练习任务\n" +
                "[/PLAN]\n\n" +
                "字段说明：\n" +
                "- 节点标题: 清晰具体，如「Python列表的创建与索引」\n" +
                "- nodeType: new_learn(新学)/review(复习)/reinforce(强化)\n" +
                "- estimatedMinutes: 建议时长(分钟)\n" +
                "- reason: 为什么学这个，与知识依赖关系对应\n" +
                "- knowledgePointId: 对应知识点ID，多个用逗号分隔\n" +
                "- 学习目标: 掌握什么，3-5个小目标\n" +
                "- 教学内容大纲: 分2-4个步骤，用 ### 分隔，每步包含讲解要点\n" +
                "- 代码示例: 针对该知识点的示范代码，用 | 包裹（无代码则填无）\n" +
                "- 练习任务: 可实践的作业题\n\n" +
                "课程ID: " + context.getCourseId() + "。回复使用中文。";
    }
}
