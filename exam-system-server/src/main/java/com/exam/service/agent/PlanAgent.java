package com.exam.service.agent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.exam.entity.KnowledgeEdge;
import com.exam.entity.KnowledgePoint;
import com.exam.service.KnowledgeService;
import com.exam.service.LlmClient;
import com.exam.service.agent.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 璺緞瑙勫垝Agent
 * 璐熻矗鏍规嵁鐢ㄦ埛鐢诲儚鍜岀煡璇嗗浘璋辩敓鎴愬涔犺矾寰? * 浣跨敤鎷撴墤鎺掑簭绠楁硶鐢熸垚鍩虹璺緞锛屽啀鐢↙LM鍋氫釜鎬у寲璋冩暣
 */
@Component
public class PlanAgent implements LearningAgent {

    private static final Logger log = LoggerFactory.getLogger(PlanAgent.class);

    @Autowired
    private LlmClient llmClient;

    @Autowired
    private KnowledgeService knowledgeService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT = "浣犳槸瀛︿範璺緞瑙勫垝鍔╂墜銆傛牴鎹敤鎴风敾鍍忓拰鐭ヨ瘑鍥捐氨鐢熸垚涓€у寲鐨勫涔犺矾寰勩€俓n" +
            "瑙勫垯锛歕n" +
            "1. 鏍规嵁鐢ㄦ埛姘村钩閫夋嫨鍚堥€傜殑璧风偣\n" +
            "2. 鎸変緷璧栧叧绯绘帓搴廫n" +
            "3. 姣忎釜鑺傜偣20-50鍒嗛挓\n" +
            "4. 鏈€澶?0涓妭鐐筡n" +
            "杩斿洖JSON锛歿\"title\":\"璺緞鏍囬\",\"courseId\":1,\"nodes\":[{\"order\":1,\"title\":\"鏍囬\",\"type\":\"review|new_learn|reinforce\",\"estimatedMinutes\":30,\"reason\":\"鍘熷洜\",\"knowledgePointIds\":[1,2]}]}";

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
        try {
            // 鏂规1锛氫娇鐢ㄦ嫇鎵戞帓搴忕畻娉曠敓鎴愬熀纭€璺緞
            AgentOutput algorithmResult = generatePathByAlgorithm(input);
            if (algorithmResult != null && "success".equals(algorithmResult.getStatus())) {
                return algorithmResult;
            }

            // 鏂规2锛氶檷绾т娇鐢↙LM鐢熸垚
            return generatePathByLLM(input);
        } catch (Exception e) {
            log.error("PlanAgent execution failed", e);
            AgentOutput output = new AgentOutput();
            output.setAgentRole(getRole());
            output.setStatus("error");
            output.setRawResponse("璺緞瑙勫垝澶辫触: " + e.getMessage());
            return output;
        }
    }

    /**
     * 浣跨敤鎷撴墤鎺掑簭绠楁硶鐢熸垚鍩虹璺緞
     */
    private AgentOutput generatePathByAlgorithm(AgentInput input) {
        try {
            AgentContext ctx = input.getContext();
            Long courseId = 1L; // 榛樿璇剧▼

            // 1. 鑾峰彇鐭ヨ瘑鍥捐氨
            Map<String, Object> graph = knowledgeService.getKnowledgeGraph(courseId);
            List<Map<String, Object>> points = (List<Map<String, Object>>) graph.get("points");
            List<Map<String, String>> edges = (List<Map<String, String>>) graph.get("edges");

            if (points == null || points.isEmpty()) {
                return null;
            }

            // 2. 鏋勫缓閭绘帴琛ㄥ拰鍏ュ害琛?            Map<Long, List<Long>> adjacencyList = new HashMap<>();
            Map<Long, Integer> inDegree = new HashMap<>();
            Map<Long, Map<String, Object>> pointMap = new HashMap<>();

            for (Map<String, Object> point : points) {
                Long id = ((Number) point.get("id")).longValue();
                pointMap.put(id, point);
                adjacencyList.put(id, new ArrayList<>());
                inDegree.put(id, 0);
            }

            // 3. 鏋勫缓鍥?            for (Map<String, String> edge : edges) {
                // 杩欓噷闇€瑕佹牴鎹甤ode鏌ユ壘id锛岀畝鍖栧鐞?                // 瀹為檯搴旇缁存姢code鍒癷d鐨勬槧灏?            }

            // 4. 鎷撴墤鎺掑簭
            Queue<Long> queue = new LinkedList<>();
            for (Map.Entry<Long, Integer> entry : inDegree.entrySet()) {
                if (entry.getValue() == 0) {
                    queue.add(entry.getKey());
                }
            }

            List<Long> sortedIds = new ArrayList<>();
            while (!queue.isEmpty()) {
                Long current = queue.poll();
                sortedIds.add(current);
                for (Long neighbor : adjacencyList.getOrDefault(current, List.of())) {
                    int newDegree = inDegree.get(neighbor) - 1;
                    inDegree.put(neighbor, newDegree);
                    if (newDegree == 0) {
                        queue.add(neighbor);
                    }
                }
            }

            // 5. 鎸夐毦搴﹀垎缁?            List<Map<String, Object>> l1Points = new ArrayList<>();
            List<Map<String, Object>> l2Points = new ArrayList<>();
            List<Map<String, Object>> l3Points = new ArrayList<>();

            for (Long id : sortedIds) {
                Map<String, Object> point = pointMap.get(id);
                String difficulty = (String) point.get("difficulty");
                if ("L1".equals(difficulty)) {
                    l1Points.add(point);
                } else if ("L2".equals(difficulty)) {
                    l2Points.add(point);
                } else {
                    l3Points.add(point);
                }
            }

            // 6. 鍚堝苟骞堕檺鍒惰妭鐐规暟
            List<Map<String, Object>> allPoints = new ArrayList<>();
            allPoints.addAll(l1Points);
            allPoints.addAll(l2Points);
            allPoints.addAll(l3Points);

            // 闄愬埗鏈€澶?0涓妭鐐?            if (allPoints.size() > 10) {
                allPoints = allPoints.subList(0, 10);
            }

            // 7. 鏋勫缓璺緞鎶ュ憡
            PlanReport report = new PlanReport();
            report.setTitle("Java瀛︿範璺緞");
            report.setCourseId(courseId);

            List<PlanReport.PathNodeInfo> nodes = new ArrayList<>();
            for (int i = 0; i < allPoints.size(); i++) {
                Map<String, Object> point = allPoints.get(i);
                PlanReport.PathNodeInfo node = new PlanReport.PathNodeInfo();
                node.setOrder(i + 1);
                node.setTitle((String) point.get("name"));
                node.setType("new_learn");
                node.setEstimatedMinutes(30);
                node.setReason("鐭ヨ瘑鐐癸細" + point.get("name"));
                node.setKnowledgePointIds(List.of(((Number) point.get("id")).longValue()));
                nodes.add(node);
            }
            report.setNodes(nodes);

            // 8. 杩斿洖缁撴灉
            AgentOutput output = new AgentOutput();
            output.setAgentRole(getRole());
            output.setStatus("success");
            output.setStructuredData(objectMapper.valueToTree(report));
            output.setRawResponse(objectMapper.writeValueAsString(report));

            log.info("PlanAgent: Generated path with {} nodes using algorithm", nodes.size());
            return output;
        } catch (Exception e) {
            log.warn("Algorithm path generation failed, falling back to LLM", e);
            return null;
        }
    }

    /**
     * 浣跨敤LLM鐢熸垚璺緞锛堥檷绾ф柟妗堬級
     */
    private AgentOutput generatePathByLLM(AgentInput input) {
        try {
            String systemPrompt = buildSystemPrompt(input.getContext());
            String userPrompt = input.getUserMessage();

            String response = llmClient.chat(systemPrompt, List.of(Map.of("role", "user", "content", userPrompt)));

            String jsonStr = extractJson(response);
            JSONObject parsed = JSON.parseObject(jsonStr);

            JsonNode structuredData = objectMapper.readTree(jsonStr);

            AgentOutput output = new AgentOutput();
            output.setAgentRole(getRole());
            output.setStatus("success");
            output.setStructuredData(structuredData);
            output.setRawResponse(response);

            log.info("PlanAgent: Generated path using LLM");
            return output;
        } catch (Exception e) {
            log.error("LLM path generation failed", e);
            AgentOutput output = new AgentOutput();
            output.setAgentRole(getRole());
            output.setStatus("error");
            output.setRawResponse("璺緞瑙勫垝澶辫触: " + e.getMessage());
            return output;
        }
    }

    /**
     * 瑙ｆ瀽璺緞鎶ュ憡
     */
    public PlanReport parsePlanReport(AgentOutput output) {
        try {
            JsonNode data = output.getStructuredData();
            return objectMapper.treeToValue(data, PlanReport.class);
        } catch (Exception e) {
            log.error("Failed to parse plan report", e);
            return null;
        }
    }

    /**
     * 鏋勫缓鍖呭惈鐢诲儚鍜岀煡璇嗗浘璋辩殑鐢ㄦ埛鎻愮ず
     */
    public String buildPromptWithProfileAndGraph(String profileJson, String graphJson) {
        return "鐢ㄦ埛鐢诲儚锛歕n" + profileJson + "\n\n鐭ヨ瘑鍥捐氨锛歕n" + graphJson;
    }

    /**
     * 浠庡搷搴斾腑鎻愬彇JSON
     */
    private String extractJson(String text) {
        int s = text.indexOf("```json"), e = text.lastIndexOf("```");
        if (s != -1 && e > s) return text.substring(s + 7, e).trim();
        s = text.indexOf("{");
        e = text.lastIndexOf("}");
        if (s != -1 && e > s) return text.substring(s, e + 1).trim();
        return text;
    }
}
