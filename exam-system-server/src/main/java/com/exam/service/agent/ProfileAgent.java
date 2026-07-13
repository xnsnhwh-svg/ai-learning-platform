package com.exam.service.agent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
import java.util.stream.Collectors;

/**
 * 鐢诲儚鏋勫缓Agent
 * 璐熻矗閫氳繃瀵硅瘽浜嗚В鐢ㄦ埛瀛︿範姘村钩锛屾瀯寤哄涔犵敾鍍? */
@Component
public class ProfileAgent implements LearningAgent {

    private static final Logger log = LoggerFactory.getLogger(ProfileAgent.class);

    @Autowired
    private LlmClient llmClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT = "浣犳槸涓€涓涔犵敾鍍忔瀯寤哄姪鎵嬨€傞€氳繃瀵硅瘽浜嗚В鐢ㄦ埛鐨勫涔犳按骞冲拰鍋忓ソ銆俓n" +
            "瑙勫垯锛堝繀椤讳弗鏍奸伒瀹堬級锛歕n" +
            "1. 姣忔鍙棶涓€涓棶棰榎n" +
            "2. 鎬诲叡鍙棶鎭板ソ3涓棶棰橈紝涓嶈兘澶氫篃涓嶈兘灏慭n" +
            "3. 鍓?涓棶棰樿繑鍥炵姸鎬乸rofiling锛岀3涓棶棰樼敤鎴峰洖绛斿悗锛岀4娆¤皟鐢ㄦ椂蹇呴』杩斿洖status:complete\n" +
            "4. 闇€瑕佷簡瑙ｏ細缂栫▼鍩虹銆佸涔犵洰鏍囥€佹瘡鍛ㄥ涔犳椂闀縗n" +
            "杩樺湪鎻愰棶鏃惰繑鍥烇細{\"status\":\"profiling\",\"type\":\"question\",\"text\":\"闂\",\"options\":[\"閫夐」1\",\"閫夐」2\",\"閫夐」3\",\"閫夐」4\"]}\n" +
            "鐢诲儚瀹屾垚鏃讹紙绗?涓棶棰樺洖绛斿悗锛夊繀椤昏繑鍥烇細{\"status\":\"complete\",\"profile\":{\"level\":\"鍒濈骇|涓骇|楂樼骇\",\"knowledge_map\":{\"java_basics\":{\"level\":0.5,\"label\":\"Java鍩虹\"}},\"cognitive_style\":{\"type\":\"depth_first\",\"avg_session_min\":30},\"weak_points\":[{\"topic\":\"钖勫急鐐筡",\"level\":0.3}],\"learning_goal\":{\"target\":\"瀛︿範鐩爣\"}}}";

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
        try {
            String systemPrompt = buildSystemPrompt(input.getContext());
            List<Map<String, String>> messages = input.getConversationHistory();

            String response = llmClient.chat(systemPrompt, messages);
            log.info("ProfileAgent raw response: {}", response);

            String jsonStr = extractJson(response);
            log.info("ProfileAgent extracted JSON: {}", jsonStr);

            // 楠岃瘉鎻愬彇鐨勫唴瀹规槸鏈夋晥鐨凧SON
            JSONObject parsed;
            JsonNode structuredData;
            try {
                parsed = JSON.parseObject(jsonStr);
                structuredData = objectMapper.readTree(jsonStr);
            } catch (Exception jsonEx) {
                log.warn("Extracted content is not valid JSON, attempting to build profile from text response");
                // 濡傛灉涓嶆槸鏈夋晥JSON锛屽皾璇曚粠鏂囨湰涓瀯寤虹敾鍍?                return buildFallbackProfile(response);
            }

            boolean isComplete = "complete".equals(parsed.getString("status"));

            AgentOutput output = new AgentOutput();
            output.setAgentRole(getRole());
            output.setStatus(isComplete ? "complete" : "profiling");
            output.setStructuredData(structuredData);
            output.setRawResponse(response);

            return output;
        } catch (Exception e) {
            log.error("ProfileAgent execution failed", e);
            AgentOutput output = new AgentOutput();
            output.setAgentRole(getRole());
            output.setStatus("error");
            output.setRawResponse("鐢诲儚鏋勫缓澶辫触: " + e.getMessage());
            return output;
        }
    }

    /**
     * 褰揓SON瑙ｆ瀽澶辫触鏃讹紝浠庢枃鏈搷搴旀瀯寤哄鐢ㄧ敾鍍?     */
    private AgentOutput buildFallbackProfile(String response) {
        log.info("Building fallback profile from response");
        
        // 妫€娴嬫槸鍚﹀寘鍚畬鎴愭爣蹇?        boolean isComplete = response.contains("complete") || response.contains("瀹屾垚") || response.contains("鐢诲儚");
        
        AgentOutput output = new AgentOutput();
        output.setAgentRole(getRole());
        
        if (isComplete) {
            // 鏋勫缓涓€涓粯璁ょ殑瀹屾垚鐢诲儚
            JSONObject forcedProfile = new JSONObject();
            forcedProfile.put("status", "complete");
            JSONObject profileData = new JSONObject();
            profileData.put("level", "鍒濈骇");
            JSONObject knowledgeMap = new JSONObject();
            JSONObject javaBasics = new JSONObject();
            javaBasics.put("level", 0.5);
            javaBasics.put("label", "Java鍩虹");
            knowledgeMap.put("java_basics", javaBasics);
            profileData.put("knowledge_map", knowledgeMap);
            JSONObject cognitiveStyle = new JSONObject();
            cognitiveStyle.put("type", "depth_first");
            cognitiveStyle.put("avg_session_min", 30);
            profileData.put("cognitive_style", cognitiveStyle);
            profileData.put("weak_points", new JSONArray());
            JSONObject learningGoal = new JSONObject();
            learningGoal.put("target", "瀛︿範Java");
            profileData.put("learning_goal", learningGoal);
            forcedProfile.put("profile", profileData);
            
            output.setStatus("complete");
            try {
                output.setStructuredData(objectMapper.readTree(forcedProfile.toJSONString()));
            } catch (Exception e) {
                log.error("Failed to parse fallback profile", e);
            }
            output.setRawResponse(forcedProfile.toJSONString());
        } else {
            // 缁х画鎻愰棶鐘舵€?            JSONObject questionJson = new JSONObject();
            questionJson.put("status", "profiling");
            questionJson.put("type", "question");
            questionJson.put("text", response.length() > 100 ? "璇峰憡璇夋垜鎮ㄧ殑瀛︿範鍩虹鍜岀洰鏍? : response);
            JSONArray options = new JSONArray();
            options.add("闆跺熀纭€");
            options.add("鏈夌紪绋嬪熀纭€");
            options.add("鏈変竴瀹氱粡楠?);
            options.add("楂樼骇寮€鍙戣€?);
            questionJson.put("options", options);
            
            output.setStatus("profiling");
            try {
                output.setStructuredData(objectMapper.readTree(questionJson.toJSONString()));
            } catch (Exception e) {
                log.error("Failed to parse fallback question", e);
            }
            output.setRawResponse(questionJson.toJSONString());
        }
        
        return output;
    }

    /**
     * 鏋勫缓寮哄埗瀹屾垚鐨勭敾鍍忥紙褰撳璇濊疆娆¤秴闄愭椂浣跨敤锛?     */
    public AgentOutput buildForcedProfile() {
        JSONObject forcedProfile = new JSONObject();
        forcedProfile.put("status", "complete");
        JSONObject profileData = new JSONObject();
        profileData.put("level", "鍒濈骇");
        JSONObject knowledgeMap = new JSONObject();
        JSONObject javaBasics = new JSONObject();
        javaBasics.put("level", 0.5);
        javaBasics.put("label", "Java鍩虹");
        knowledgeMap.put("java_basics", javaBasics);
        profileData.put("knowledge_map", knowledgeMap);
        JSONObject cognitiveStyle = new JSONObject();
        cognitiveStyle.put("type", "depth_first");
        cognitiveStyle.put("avg_session_min", 30);
        profileData.put("cognitive_style", cognitiveStyle);
        profileData.put("weak_points", new JSONArray());
        JSONObject learningGoal = new JSONObject();
        learningGoal.put("target", "瀛︿範Java");
        profileData.put("learning_goal", learningGoal);
        forcedProfile.put("profile", profileData);

        AgentOutput output = new AgentOutput();
        output.setAgentRole(getRole());
        output.setStatus("complete");
        try {
            output.setStructuredData(objectMapper.readTree(forcedProfile.toJSONString()));
        } catch (Exception e) {
            log.error("Failed to parse forced profile", e);
        }
        output.setRawResponse(forcedProfile.toJSONString());
        return output;
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
