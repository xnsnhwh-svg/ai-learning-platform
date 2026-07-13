package com.exam.service.rag;

import com.exam.dto.rag.RagResultDTO;
import com.exam.entity.AgentMessage;
import com.exam.entity.GeneratedResource;
import com.exam.entity.KnowledgePoint;
import com.exam.mapper.AgentMessageMapper;
import com.exam.mapper.GeneratedResourceMapper;
import com.exam.mapper.KnowledgePointMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final KnowledgePointMapper knowledgePointMapper;
    private final GeneratedResourceMapper generatedResourceMapper;
    private final AgentMessageMapper agentMessageMapper;
    private final WebClient webClient;

    @Value("${deepseek.api.base-url}")
    private String baseUrl;
    @Value("${deepseek.api.api-key}")
    private String apiKey;
    @Value("${deepseek.api.model}")
    private String model;

    public RagService(KnowledgePointMapper knowledgePointMapper,
                      GeneratedResourceMapper generatedResourceMapper,
                      AgentMessageMapper agentMessageMapper) {
        this.knowledgePointMapper = knowledgePointMapper;
        this.generatedResourceMapper = generatedResourceMapper;
        this.agentMessageMapper = agentMessageMapper;
        this.webClient = WebClient.builder().build();
    }

    public RagResultDTO query(String query, Long courseId, int topK) {
        List<String> keywords = extractKeywords(query);
        List<RagResultDTO.Source> candidates = new ArrayList<>();

        candidates.addAll(searchKnowledgePoints(keywords, courseId));
        candidates.addAll(searchGeneratedResources(keywords));
        candidates.addAll(searchMessages(keywords));

        candidates.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        List<RagResultDTO.Source> topSources = candidates.subList(0, Math.min(topK, candidates.size()));

        String context = buildContext(topSources);
        String answer = llmAnswer(query, context);

        RagResultDTO result = new RagResultDTO();
        result.setAnswer(answer);
        result.setSources(topSources);
        return result;
    }

    private List<String> extractKeywords(String query) {
        String cleaned = query.toLowerCase()
                .replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9 ]", " ");
        String[] words = cleaned.split("\\s+");
        Set<String> stopWords = Set.of("的", "了", "是", "在", "有", "和", "就", "不", "人", "都",
                "一", "一个", "上", "也", "很", "到", "说", "要", "去", "你", "会", "着",
                "没有", "看", "好", "自己", "这", "吗", "吧", "啊", "呢", "what", "how",
                "why", "when", "is", "the", "a", "an", "are", "was", "were", "do", "does",
                "did", "can", "could", "will", "would", "should", "may", "might", "shall",
                "i", "you", "he", "she", "it", "we", "they", "my", "your", "his", "her",
                "its", "our", "their", "me", "him", "us", "them");
        return Arrays.stream(words)
                .filter(w -> w.length() > 1 && !stopWords.contains(w))
                .distinct()
                .collect(Collectors.toList());
    }

    private List<RagResultDTO.Source> searchKnowledgePoints(List<String> keywords, Long courseId) {
        List<KnowledgePoint> kps;
        if (courseId != null) {
            kps = knowledgePointMapper.selectByCourseId(courseId);
        } else {
            kps = knowledgePointMapper.selectList(null);
        }

        List<RagResultDTO.Source> results = new ArrayList<>();
        for (KnowledgePoint kp : kps) {
            StringBuilder text = new StringBuilder();
            text.append(kp.getName()).append(" ");
            text.append(kp.getKeywords() != null ? kp.getKeywords() : "").append(" ");
            text.append(kp.getDescription() != null ? kp.getDescription() : "");

            double score = scoreText(text.toString(), keywords);
            if (score > 0) {
                RagResultDTO.Source s = new RagResultDTO.Source();
                s.setId("kp-" + kp.getId());
                s.setTitle(kp.getName());
                s.setSnippet(kp.getDescription() != null
                        ? truncate(kp.getDescription(), 200) : kp.getName());
                s.setType("knowledge_point");
                s.setScore(score);
                results.add(s);
            }
        }
        return results;
    }

    private List<RagResultDTO.Source> searchGeneratedResources(List<String> keywords) {
        List<GeneratedResource> resources = generatedResourceMapper.selectList(null);
        List<RagResultDTO.Source> results = new ArrayList<>();

        for (GeneratedResource r : resources) {
            StringBuilder text = new StringBuilder();
            text.append(r.getTitle()).append(" ");
            if (r.getContentJson() != null) {
                text.append(r.getContentJson().path("markdown").asText("")).append(" ");
            }

            double score = scoreText(text.toString(), keywords);
            if (score > 0) {
                RagResultDTO.Source s = new RagResultDTO.Source();
                s.setId("res-" + r.getId());
                s.setTitle(r.getTitle());
                s.setSnippet(truncate(text.toString(), 200));
                s.setType("resource");
                s.setScore(score);
                results.add(s);
            }
        }
        return results;
    }

    private List<RagResultDTO.Source> searchMessages(List<String> keywords) {
        List<AgentMessage> all = agentMessageMapper.selectList(null);
        List<RagResultDTO.Source> results = new ArrayList<>();

        for (AgentMessage m : all) {
            if (m.getContent() == null || m.getContent().isBlank()) continue;

            double score = scoreText(m.getContent(), keywords);
            if (score > 0) {
                RagResultDTO.Source s = new RagResultDTO.Source();
                s.setId("msg-" + m.getId());
                s.setTitle("对话记录 - " + m.getRole());
                s.setSnippet(truncate(m.getContent(), 200));
                s.setType("conversation");
                s.setScore(score);
                results.add(s);
            }
        }
        return results;
    }

    private double scoreText(String text, List<String> keywords) {
        if (text == null || text.isEmpty()) return 0;
        String lower = text.toLowerCase();
        double score = 0;
        for (String kw : keywords) {
            int count = countOccurrences(lower, kw.toLowerCase());
            if (count > 0) {
                // TF: term frequency, with length normalization
                score += count * (1.0 / (1 + Math.log(lower.length() / 100.0 + 1)));
            }
        }
        return score;
    }

    private int countOccurrences(String text, String keyword) {
        int count = 0, idx = 0;
        while ((idx = text.indexOf(keyword, idx)) != -1) {
            count++;
            idx += keyword.length();
        }
        return count;
    }

    private String buildContext(List<RagResultDTO.Source> sources) {
        StringBuilder sb = new StringBuilder();
        sb.append("以下是从知识库中检索到的相关片段，请基于这些内容回答问题：\n\n");
        for (int i = 0; i < sources.size(); i++) {
            RagResultDTO.Source s = sources.get(i);
            sb.append("--- 片段 ").append(i + 1).append(" [").append(s.getType()).append("] ---\n");
            sb.append("标题：").append(s.getTitle()).append("\n");
            sb.append("内容：").append(s.getSnippet()).append("\n\n");
        }
        return sb.toString();
    }

    private String llmAnswer(String query, String context) {
        try {
            String prompt = "你是一个基于知识库回答问题的学习助手。\n\n"
                    + context
                    + "\n用户问题：" + query + "\n\n"
                    + "请基于以上知识库内容回答问题。引用来源时用 [1][2] 标注。"
                    + "如果知识库中没有相关信息，如实说明。";

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("max_tokens", 2048);
            body.put("temperature", 0.3);
            body.put("stream", false);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", "你是一个严谨的学习助手，严格基于提供的知识库内容回答问题。"));
            messages.add(Map.of("role", "user", "content", prompt));
            body.put("messages", messages);

            String response = webClient.post()
                    .uri(baseUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseLLMResponse(response);
        } catch (Exception e) {
            return "RAG 回答生成失败: " + e.getMessage();
        }
    }

    private String parseLLMResponse(String response) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            var root = mapper.readTree(response);
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            return "无法解析 LLM 响应";
        }
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "...";
    }
}
