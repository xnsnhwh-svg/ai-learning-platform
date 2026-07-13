package com.exam.service.agent.prompt;

import com.exam.service.agent.model.GenContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class GenPromptBuilder implements PromptBuilder {

    @Override
    public String getPhase() { return "generating"; }

    public List<Map<String, String>> build(GenContext context) {
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
                "你是个性化学习资源生成专家。生成以下类型资源：\n" +
                "- document（学习文档）\n- quiz（测验）\n- mindmap（思维导图）\n\n" +
                "输出格式：\n[RESOURCES]\nTYPE: document\nTITLE: 标题\nCONTENT: Markdown内容\nDIFFICULTY: L1\n---\n" +
                "TYPE: quiz\nTITLE: 测验\nCONTENT: 题目\nDIFFICULTY: L1\n[/RESOURCES]"));
        if (history != null) messages.addAll(history);
        messages.add(Map.of("role", "user", "content", userMessage));
        return messages;
    }

    private String buildSystemPrompt(GenContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是个性化学习资源生成专家。根据学生学习目标和路径，生成定制化学习资源。\n\n")
          .append("可生成类型：\n")
          .append("1. document - 学习文档（Markdown格式）\n")
          .append("2. quiz - 测验题目（选择题/判断题）\n")
          .append("3. mindmap - 思维导图\n")
          .append("4. reading - 阅读材料\n")
          .append("5. video_script - 视频脚本\n\n")
          .append("输出格式：\n[RESOURCES]\nNODE_ID: <节点ID>\nKP_ID: <知识点ID>\nTYPE: document\nTITLE: 标题\nCONTENT: 内容\nDIFFICULTY: L1\n---\n...\n[/RESOURCES]\n\n")
          .append("用户ID: ").append(context.getUserId()).append("\n");
        if (context.getCourseId() != null) {
            sb.append("课程ID: ").append(context.getCourseId()).append("\n");
        }
        if (context.getPathNodes() != null && !context.getPathNodes().isEmpty()) {
            sb.append("\n当前学习路径节点（请为每个节点生成对应资源，并在资源块中标注NODE_ID和KP_ID）：\n");
            for (com.exam.entity.PathNode node : context.getPathNodes()) {
                sb.append("- 节点ID:").append(node.getId())
                  .append(", 标题:").append(node.getTitle())
                  .append(", 类型:").append(node.getNodeType())
                  .append("\n");
            }
        }
        sb.append("回复使用中文。");
        return sb.toString();
    }
}
