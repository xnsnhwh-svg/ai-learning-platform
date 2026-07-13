package com.exam.service.agent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.exam.entity.GeneratedResource;
import com.exam.service.GeneratedResourceService;
import com.exam.service.LlmClient;
import com.exam.service.agent.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 璧勬簮鐢熸垚Agent
 * 璐熻矗鏍规嵁鐭ヨ瘑鐐圭敓鎴愬涔犺祫婧愶紙鏂囨。銆佺粌涔犻銆佹€濈淮瀵煎浘锛? * 浼樺厛浣跨敤棰勫瓨鐨勮祫婧愶紝娌℃湁鍐嶈皟LLM鐢熸垚
 */
@Component
public class GenAgent implements LearningAgent {

    private static final Logger log = LoggerFactory.getLogger(GenAgent.class);

    @Autowired
    private LlmClient llmClient;

    @Autowired
    private GeneratedResourceService generatedResourceService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT = "浣犳槸瀛︿範璧勬簮鐢熸垚鍔╂墜銆傛牴鎹姹傜敓鎴愬涔犺祫鏂欍€俓n" +
            "鏂囨。锛歿\"type\":\"document\",\"title\":\"鏍囬\",\"content\":\"# 鏍囬\\n\\nMarkdown鍐呭\"}\n" +
            "缁冧範棰橈細{\"type\":\"quiz\",\"title\":\"鏍囬\",\"content\":{\"questions\":[{\"content\":\"棰樼洰\",\"type\":\"CHOICE\",\"difficulty\":\"EASY\",\"options\":[{\"content\":\"A\",\"isCorrect\":true}],\"analysis\":\"瑙ｆ瀽\"}]}}\n" +
            "鎬濈淮瀵煎浘锛歿\"type\":\"mindmap\",\"title\":\"鏍囬\",\"content\":{\"topic\":\"涓婚\",\"children\":[{\"topic\":\"瀛愪富棰榎"}]}}";

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
        try {
            String systemPrompt = buildSystemPrompt(input.getContext());
            String userPrompt = input.getUserMessage();

            String response = llmClient.chat(systemPrompt, List.of(Map.of("role", "user", "content", userPrompt)));
            log.info("GenAgent raw response: {}", response);

            String jsonStr = extractJson(response);
            log.info("GenAgent extracted JSON: {}", jsonStr);

            JsonNode structuredData;
            try {
                structuredData = objectMapper.readTree(jsonStr);
            } catch (Exception jsonEx) {
                log.warn("JSON瑙ｆ瀽澶辫触锛屽皾璇曟瀯寤洪粯璁よ祫婧? {}", jsonEx.getMessage());
                // 鏋勫缓榛樿璧勬簮
                structuredData = buildDefaultResource(input.getUserMessage(), jsonStr);
            }

            AgentOutput output = new AgentOutput();
            output.setAgentRole(getRole());
            output.setStatus("success");
            output.setStructuredData(structuredData);
            output.setRawResponse(response);

            return output;
        } catch (Exception e) {
            log.error("GenAgent execution failed", e);
            AgentOutput output = new AgentOutput();
            output.setAgentRole(getRole());
            output.setStatus("error");
            output.setRawResponse("璧勬簮鐢熸垚澶辫触: " + e.getMessage());
            return output;
        }
    }

    /**
     * 褰揓SON瑙ｆ瀽澶辫触鏃讹紝鏋勫缓榛樿璧勬簮
     */
    private JsonNode buildDefaultResource(String prompt, String rawText) {
        try {
            // 鏍规嵁prompt鍒ゆ柇璧勬簮绫诲瀷
            if (prompt.contains("缁冧範棰?) || prompt.contains("quiz")) {
                JSONObject quiz = new JSONObject();
                quiz.put("title", "缁冧範棰?);
                quiz.put("type", "quiz");
                JSONObject content = new JSONObject();
                JSONArray questions = new JSONArray();
                JSONObject q1 = new JSONObject();
                q1.put("content", "鍏充簬杩欎釜鐭ヨ瘑鐐癸紝浠ヤ笅璇存硶姝ｇ‘鐨勬槸锛?);
                q1.put("type", "CHOICE");
                q1.put("difficulty", "EASY");
                JSONArray options = new JSONArray();
                options.add(new JSONObject() {{ put("content", "閫夐」A"); put("isCorrect", true); }});
                options.add(new JSONObject() {{ put("content", "閫夐」B"); put("isCorrect", false); }});
                q1.put("options", options);
                q1.put("analysis", "璇锋牴鎹疄闄呭唴瀹归€夋嫨姝ｇ‘绛旀");
                questions.add(q1);
                content.put("questions", questions);
                quiz.put("content", content);
                return objectMapper.readTree(quiz.toJSONString());
            } else if (prompt.contains("鎬濈淮瀵煎浘") || prompt.contains("mindmap")) {
                JSONObject mindmap = new JSONObject();
                mindmap.put("title", "鎬濈淮瀵煎浘");
                mindmap.put("type", "mindmap");
                JSONObject content = new JSONObject();
                content.put("topic", "鐭ヨ瘑鐐?);
                content.put("children", new JSONArray());
                mindmap.put("content", content);
                return objectMapper.readTree(mindmap.toJSONString());
            } else {
                // 榛樿鏂囨。
                JSONObject doc = new JSONObject();
                doc.put("title", "瀛︿範鏂囨。");
                doc.put("type", "document");
                doc.put("content", rawText.length() > 500 ? rawText.substring(0, 500) + "..." : rawText);
                return objectMapper.readTree(doc.toJSONString());
            }
        } catch (Exception e) {
            log.error("Failed to build default resource", e);
            // 杩斿洖鏈€鍩烘湰鐨凧SON
            try {
                return objectMapper.readTree("{\"title\":\"璧勬簮\",\"content\":\"鍐呭鐢熸垚涓?..\"}");
            } catch (Exception ex) {
                return null;
            }
        }
    }

    /**
     * 涓烘寚瀹氳妭鐐圭敓鎴愬崟涓祫婧?     * 浼樺厛浣跨敤棰勫瓨璧勬簮锛屾病鏈夊啀璋僉LM
     */
    public AgentOutput generateResource(String nodeTitle, String resourceType, String kpInfo, Long pathNodeId) {
        // 1. 鍏堟鏌ユ槸鍚︽湁棰勫瓨璧勬簮锛堟牴鎹爣棰樺尮閰嶏級
        GeneratedResource existing = findExistingResourceByTitle(nodeTitle, resourceType);
        if (existing != null) {
            log.info("Using pre-existing resource for node: {}, type: {}", nodeTitle, resourceType);
            AgentOutput output = new AgentOutput();
            output.setAgentRole(getRole());
            output.setStatus("success");
            output.setStructuredData(existing.getContentJson());
            output.setRawResponse("浣跨敤棰勫瓨璧勬簮");
            return output;
        }

        // 2. 娌℃湁棰勫瓨璧勬簮锛岃皟LLM鐢熸垚
        log.info("Generating new resource for node: {}, type: {}", nodeTitle, resourceType);
        String typeDesc = switch (resourceType) {
            case "document" -> "Markdown鏍煎紡鐨勮瑙ｆ枃妗?;
            case "quiz" -> "3閬撶粌涔犻";
            case "mindmap" -> "鎬濈淮瀵煎浘鏍戝舰JSON";
            default -> resourceType;
        };
        String prompt = "璇蜂负浠ヤ笅鑺傜偣鐢熸垚" + typeDesc + "锛歕n鑺傜偣锛? + nodeTitle + "\n鐭ヨ瘑鐐癸細" + (kpInfo.isEmpty() ? "鏃? : kpInfo);

        AgentInput input = new AgentInput();
        input.setUserMessage(prompt);
        return execute(input);
    }

    /**
     * 鏍规嵁鏍囬鏌ユ壘宸插瓨鍦ㄧ殑璧勬簮
     */
    private GeneratedResource findExistingResourceByTitle(String nodeTitle, String resourceType) {
        try {
            // 浣跨敤妯＄硦鍖归厤鏍囬
            List<GeneratedResource> resources = generatedResourceService.getResourcesByTitle(nodeTitle);
            return resources.stream()
                    .filter(r -> resourceType.equals(r.getResourceType()))
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Failed to find existing resource by title", e);
            return null;
        }
    }

    /**
     * 瑙ｆ瀽璧勬簮鎶ュ憡
     */
    public ResourceReport parseResourceReport(AgentOutput output, Long pathNodeId, String resourceType) {
        try {
            JsonNode data = output.getStructuredData();
            ResourceReport report = new ResourceReport();
            report.setPathNodeId(pathNodeId);
            report.setResourceType(resourceType);
            report.setTitle(data.has("title") ? data.get("title").asText() : "");
            report.setContent(data.has("content") ? data.get("content") : data);
            report.setDifficulty("L1");
            return report;
        } catch (Exception e) {
            log.error("Failed to parse resource report", e);
            return null;
        }
    }

    /**
     * 浠庡搷搴斾腑鎻愬彇JSON
     */
    private String extractJson(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        // 灏濊瘯浠?```json ... ``` 浠ｇ爜鍧椾腑鎻愬彇
        int s = text.indexOf("```json");
        int e = text.indexOf("```", s + 7);
        if (s != -1 && e > s) {
            return text.substring(s + 7, e).trim();
        }
        
        // 灏濊瘯浠?``` ... ``` 浠ｇ爜鍧椾腑鎻愬彇
        s = text.indexOf("```");
        e = text.indexOf("```", s + 3);
        if (s != -1 && e > s) {
            String candidate = text.substring(s + 3, e).trim();
            if (candidate.startsWith("{") || candidate.startsWith("[")) {
                return candidate;
            }
        }
        
        // 灏濊瘯鎵惧埌瀹屾暣鐨凧SON瀵硅薄锛堜粠绗竴涓獅鍒版渶鍚庝竴涓獇锛?        s = text.indexOf("{");
        if (s != -1) {
            // 鎵惧埌鍖归厤鐨勭粨鏉熸嫭鍙?            int depth = 0;
            for (int i = s; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '{') depth++;
                else if (c == '}') depth--;
                if (depth == 0) {
                    return text.substring(s, i + 1).trim();
                }
            }
        }
        
        // 濡傛灉閮芥病鏈夋壘鍒帮紝杩斿洖鍘熸枃
        return text;
    }
}
