package com.exam.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.exam.entity.*;
import com.exam.mapper.AgentMessageMapper;
import com.exam.mapper.AgentSessionMapper;
import com.exam.service.*;
import com.exam.service.agent.*;
import com.exam.service.agent.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class AgentOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestratorService.class);

    @Autowired private LlmClient llmClient;
    @Autowired private ProfileAgent profileAgent;
    @Autowired private PlanAgent planAgent;
    @Autowired private GenAgent genAgent;
    @Autowired private SynthesisEngine synthesisEngine;
    @Autowired private ConflictDetector conflictDetector;
    @Autowired private AgentSessionMapper agentSessionMapper;
    @Autowired private AgentMessageMapper agentMessageMapper;
    @Autowired private KnowledgeService knowledgeService;
    @Autowired private UserProfileService userProfileService;
    @Autowired private LearningPathService learningPathService;
    @Autowired private GeneratedResourceService generatedResourceService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    // ==================== 主入口 ====================

    public void handleChat(Long sessionId, String message, Long userId, SseEmitter emitter) {
        try {
            AgentSession session = getOrCreateSession(sessionId, message, userId);
            saveUserMessage(session.getId(), message);
            List<AgentMessage> history = agentMessageMapper.selectBySessionId(session.getId());

            String phase = determinePhase(history);
            log.info("Session {} in phase: {}", session.getId(), phase);

            switch (phase) {
                case "profiling" -> handleProfiling(session, history, emitter);
                case "planning" -> handlePlanning(session, userId, emitter);
                case "generating" -> handleGenerating(session, userId, emitter);
                default -> sendDone(emitter, session.getId());
            }
        } catch (Exception e) {
            log.error("Agent chat error", e);
            sendError(emitter, "处理失败: " + e.getMessage());
        } finally {
            emitter.complete();
        }
    }

    // ==================== A3.1 意图分类 ====================

    private OrchestrationPlan classifyIntent(String userMessage, List<AgentMessage> history) {
        try {
            String prompt = "分析以下用户请求，判断任务复杂度并列出需要的Agent。\n" +
                    "可用Agent: profile(画像构建), plan(路径规划), generate(资源生成)\n" +
                    "简单任务示例: \"出10道Java题目\" -> {\"complexity\":\"simple\",\"plan\":[{\"step\":1,\"agent\":\"generate\",\"task\":\"生成Java题目\",\"depends_on\":[]}]}\n" +
                    "复杂任务示例: \"从零教我Spring Boot\" -> {\"complexity\":\"complex\",\"plan\":[{\"step\":1,\"agent\":\"profile\",\"task\":\"分析用户水平\",\"depends_on\":[]},{\"step\":2,\"agent\":\"plan\",\"task\":\"规划学习路径\",\"depends_on\":[1]},{\"step\":3,\"agent\":\"generate\",\"task\":\"生成学习资源\",\"depends_on\":[2],\"parallel\":true}]}\n" +
                    "用户请求: " + userMessage;

            String response = llmClient.chat("你是一个任务分析助手，只输出JSON。", List.of(Map.of("role", "user", "content", prompt)));
            String jsonStr = extractJson(response);
            return JSON.parseObject(jsonStr, OrchestrationPlan.class);
        } catch (Exception e) {
            log.warn("意图分类失败，使用默认流程", e);
            return defaultPlan();
        }
    }

    private OrchestrationPlan defaultPlan() {
        OrchestrationPlan plan = new OrchestrationPlan();
        plan.setComplexity("complex");
        List<OrchestrationPlan.PlanStep> steps = new ArrayList<>();
        OrchestrationPlan.PlanStep s1 = new OrchestrationPlan.PlanStep();
        s1.setStep(1); s1.setAgent("profile"); s1.setTask("分析用户水平"); s1.setDependsOn(Collections.emptyList());
        steps.add(s1);
        OrchestrationPlan.PlanStep s2 = new OrchestrationPlan.PlanStep();
        s2.setStep(2); s2.setAgent("plan"); s2.setTask("规划学习路径"); s2.setDependsOn(List.of(1));
        steps.add(s2);
        OrchestrationPlan.PlanStep s3 = new OrchestrationPlan.PlanStep();
        s3.setStep(3); s3.setAgent("generate"); s3.setTask("生成学习资源"); s3.setDependsOn(List.of(2)); s3.setParallel(true);
        steps.add(s3);
        plan.setPlan(steps);
        return plan;
    }

    // ==================== A3.3 并行调度 ====================

    private Map<String, AgentOutput> executePlan(OrchestrationPlan plan, AgentContext ctx) {
        Map<String, AgentOutput> results = new LinkedHashMap<>();
        Set<Integer> completed = new HashSet<>();

        for (OrchestrationPlan.PlanStep step : plan.getPlan()) {
            // 检查依赖是否满足
            boolean depsMet = step.getDependsOn() == null || step.getDependsOn().isEmpty()
                    || completed.containsAll(step.getDependsOn());

            if (!depsMet) {
                log.warn("Step {} 依赖未满足，跳过", step.getStep());
                continue;
            }

            if (step.isParallel()) {
                // 并行执行（当前只有 generate 会标记 parallel）
                CompletableFuture<AgentOutput> future = CompletableFuture.supplyAsync(() ->
                        executeAgent(step.getAgent(), ctx));
                try {
                    results.put(step.getAgent(), future.get());
                } catch (Exception e) {
                    log.error("Agent {} 并行执行失败", step.getAgent(), e);
                }
            } else {
                AgentOutput output = executeAgent(step.getAgent(), ctx);
                results.put(step.getAgent(), output);
            }
            completed.add(step.getStep());
        }

        return results;
    }

    private AgentOutput executeAgent(String agentName, AgentContext ctx) {
        AgentInput input = new AgentInput();
        input.setContext(ctx);
        input.setUserMessage(String.valueOf(ctx.getLocalContext().getOrDefault("task", "")));

        return switch (agentName) {
            case "profile" -> profileAgent.execute(input);
            case "plan" -> planAgent.execute(input);
            case "generate" -> genAgent.execute(input);
            default -> {
                AgentOutput err = new AgentOutput();
                err.setAgentRole(agentName);
                err.setStatus("failed");
                err.setStructuredData(Map.of("error", "未知Agent: " + agentName));
                yield err;
            }
        };
    }

    // ==================== profiling 多轮对话 ====================

    private void handleProfiling(AgentSession session, List<AgentMessage> history, SseEmitter emitter) throws Exception {
        sendPhase(emitter, "profiling", 0.15, null, session.getId());

        long userMsgCount = history.stream().filter(m -> "user".equals(m.getRole())).count();
        if (userMsgCount >= 3) {
            forceCompleteProfile(session, emitter);
            return;
        }

        List<Map<String, String>> messages = history.stream()
                .filter(m -> !"system".equals(m.getRole()))
                .map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
                .collect(Collectors.toList());

        AgentContext ctx = new AgentContext();
        ctx.setSessionId(String.valueOf(session.getId()));
        ctx.setUserId(String.valueOf(session.getUserId()));
        ctx.setConversationHistory(messages);

        AgentInput input = new AgentInput();
        input.setContext(ctx);
        AgentOutput output = profileAgent.execute(input);

        String rawResponse = output.getRawResponse();
        saveMessage(session.getId(), rawResponse, "profiling");

        if ("success".equals(output.getStatus())) {
            // 画像完成
            JSONObject profileData = (JSONObject) JSON.toJSON(output.getStructuredData());
            saveProfile(session.getUserId(), profileData, session.getId());
            sendPhase(emitter, "profiling", 0.30,
                    Map.of("type", "text", "text", "已完成学习画像构建！正在为您规划学习路径..."), session.getId());
            Thread.sleep(500);
            handlePlanning(session, session.getUserId(), emitter);
        } else {
            // 继续提问
            JSONObject parsed = JSON.parseObject(extractJson(rawResponse));
            Map<String, Object> content = new HashMap<>();
            content.put("type", parsed.getString("type"));
            content.put("text", parsed.getString("text"));
            if (parsed.containsKey("options")) {
                content.put("options", parsed.getJSONArray("options"));
            }
            sendPhase(emitter, "profiling", 0.20, content, session.getId());
        }
    }

    private void forceCompleteProfile(AgentSession session, SseEmitter emitter) throws Exception {
        JSONObject profileData = new JSONObject();
        profileData.put("level", "初级");
        JSONObject knowledgeMap = new JSONObject();
        JSONObject javaBasics = new JSONObject();
        javaBasics.put("level", 0.5); javaBasics.put("label", "Java基础");
        knowledgeMap.put("java_basics", javaBasics);
        profileData.put("knowledge_map", knowledgeMap);
        JSONObject cognitiveStyle = new JSONObject();
        cognitiveStyle.put("type", "depth_first"); cognitiveStyle.put("avg_session_min", 30);
        profileData.put("cognitive_style", cognitiveStyle);
        profileData.put("weak_points", new JSONArray());
        JSONObject learningGoal = new JSONObject();
        learningGoal.put("target", "学习Java");
        profileData.put("learning_goal", learningGoal);

        JSONObject forced = new JSONObject();
        forced.put("status", "complete"); forced.put("profile", profileData);
        saveProfile(session.getUserId(), profileData, session.getId());
        saveMessage(session.getId(), forced.toJSONString(), "profiling");

        sendPhase(emitter, "profiling", 0.30,
                Map.of("type", "text", "text", "已完成学习画像构建！正在为您规划学习路径..."), session.getId());
        Thread.sleep(500);
        handlePlanning(session, session.getUserId(), emitter);
    }

    // ==================== planning ====================

    private void handlePlanning(AgentSession session, Long userId, SseEmitter emitter) throws Exception {
        sendPhase(emitter, "planning", 0.40, Map.of("summary", "正在为您规划学习路径..."), session.getId());

        Map<String, Object> graph = knowledgeService.getKnowledgeGraph(1L);
        String graphJson = objectMapper.writeValueAsString(graph);

        UserProfile profile = userProfileService.getUserProfile(userId);
        String profileJson = profile != null ? objectMapper.writeValueAsString(profile) : "{}";

        String prompt = "用户画像：\n" + profileJson + "\n\n知识图谱：\n" + graphJson;

        AgentContext ctx = new AgentContext();
        ctx.setSessionId(String.valueOf(session.getId()));
        ctx.setUserId(String.valueOf(userId));
        AgentInput input = new AgentInput();
        input.setContext(ctx);
        input.setUserMessage(prompt);

        AgentOutput output = planAgent.execute(input);

        if ("success".equals(output.getStatus())) {
            PlanReport report = (PlanReport) output.getStructuredData();
            LearningPath path = new LearningPath();
            path.setUserId(userId);
            path.setCourseId(report.getCourseId() != null ? report.getCourseId() : 1L);
            path.setSessionId(session.getId());
            path.setStatus("active");

            List<PathNode> nodes = new ArrayList<>();
            if (report.getNodes() != null) {
                for (PlanReport.PlanNode pn : report.getNodes()) {
                    PathNode node = new PathNode();
                    node.setNodeOrder(pn.getOrder());
                    node.setTitle(pn.getTitle());
                    node.setNodeType(pn.getType());
                    node.setEstimatedMinutes(pn.getEstimatedMinutes());
                    node.setReason(pn.getReason());
                    node.setKnowledgePointIds(pn.getKnowledgePointIds());
                    node.setStatus("pending");
                    nodes.add(node);
                }
            }
            path.setNodes(nodes);
            learningPathService.createLearningPath(path);
        }

        saveMessage(session.getId(), output.getRawResponse(), "planning");
        sendPhase(emitter, "planning", 0.50, Map.of("summary", "已生成学习路径"), session.getId());
        Thread.sleep(500);
        handleGenerating(session, userId, emitter);
    }

    // ==================== generating ====================

    private void handleGenerating(AgentSession session, Long userId, SseEmitter emitter) throws Exception {
        List<LearningPath> paths = learningPathService.getLearningPathsByUser(userId);
        if (paths.isEmpty()) {
            sendPhase(emitter, "generating", 0.60, Map.of("chapter_title", "暂无学习路径", "current", 0, "total", 0), session.getId());
            return;
        }

        LearningPath detail = learningPathService.getLearningPathDetail(paths.get(0).getId());
        List<PathNode> nodes = detail.getNodes();

        sendPhase(emitter, "generating", 0.55,
                Map.of("chapter_title", "开始生成学习资料", "current", 0, "total", nodes.size()), session.getId());

        List<ResourceReport> allResources = new ArrayList<>();

        for (int i = 0; i < nodes.size(); i++) {
            PathNode node = nodes.get(i);
            double progress = 0.55 + (0.40 * (i + 1) / nodes.size());
            sendPhase(emitter, "generating", progress,
                    Map.of("chapter_title", "第" + (i + 1) + "章：" + node.getTitle(), "current", i + 1, "total", nodes.size()),
                    session.getId());

            String kpInfo = "";
            if (node.getKnowledgePoints() != null) {
                kpInfo = node.getKnowledgePoints().stream()
                        .map(kp -> kp.getName() + "：" + kp.getDescription())
                        .collect(Collectors.joining("\n"));
            }
            final String finalKpInfo = kpInfo;
            final PathNode finalNode = node;

            // 并行生成 3 类资源
            CompletableFuture<ResourceReport> docFuture = CompletableFuture.supplyAsync(() ->
                    genSingleResource(finalNode, "document", finalKpInfo));
            CompletableFuture<ResourceReport> quizFuture = CompletableFuture.supplyAsync(() ->
                    genSingleResource(finalNode, "quiz", finalKpInfo));
            CompletableFuture<ResourceReport> mindmapFuture = CompletableFuture.supplyAsync(() ->
                    genSingleResource(finalNode, "mindmap", finalKpInfo));

            CompletableFuture.allOf(docFuture, quizFuture, mindmapFuture).join();

            ResourceReport doc = docFuture.get();
            ResourceReport quiz = quizFuture.get();
            ResourceReport mindmap = mindmapFuture.get();

            if (doc != null) { saveGeneratedResource(session.getId(), node, doc); allResources.add(doc); }
            if (quiz != null) { saveGeneratedResource(session.getId(), node, quiz); allResources.add(quiz); }
            if (mindmap != null) { saveGeneratedResource(session.getId(), node, mindmap); allResources.add(mindmap); }
        }

        // 冲突检测
        UserProfile profile = userProfileService.getUserProfile(userId);
        ProfileReport profileReport = new ProfileReport();
        if (profile != null && profile.getDimensions() != null) {
            try {
                profileReport = objectMapper.treeToValue(profile.getDimensions(), ProfileReport.class);
            } catch (Exception e) { log.warn("解析画像失败", e); }
        }

        LearningPath latestPath = paths.get(0);
        PlanReport planReport = new PlanReport();
        planReport.setCourseId(latestPath.getCourseId());
        // Build planReport from nodes for conflict detection
        List<PlanReport.PlanNode> planNodes = nodes.stream().map(n -> {
            PlanReport.PlanNode pn = new PlanReport.PlanNode();
            pn.setOrder(n.getNodeOrder());
            pn.setTitle(n.getTitle());
            pn.setType(n.getNodeType());
            pn.setEstimatedMinutes(n.getEstimatedMinutes());
            pn.setReason(n.getReason());
            List<Long> kpIds = n.getKnowledgePointIds();
            if (kpIds != null) {
                pn.setKnowledgePointIds(new ArrayList<>(kpIds));
            } else {
                pn.setKnowledgePointIds(Collections.emptyList());
            }
            return pn;
        }).collect(Collectors.toList());
        planReport.setNodes(planNodes);

        ConflictResult conflict = conflictDetector.detect(planReport, allResources, profileReport);
        if (!conflict.isPassed()) {
            log.warn("冲突检测未通过: score={}", conflict.getScore());
        }

        saveMessage(session.getId(), "学习资料生成完成", "done");
        learningPathService.updatePathStatus(latestPath.getId(), "completed");
        session.setStatus("completed");
        agentSessionMapper.updateById(session);
        sendDone(emitter, session.getId());
    }

    private ResourceReport genSingleResource(PathNode node, String type, String kpInfo) {
        try {
            String typeDesc = switch (type) {
                case "document" -> "Markdown格式的讲解文档";
                case "quiz" -> "3道练习题";
                case "mindmap" -> "思维导图树形JSON";
                default -> type;
            };
            String prompt = "请为以下节点生成" + typeDesc + "：\n节点：" + node.getTitle() + "\n知识点：" + (kpInfo.isEmpty() ? "无" : kpInfo);

            AgentContext ctx = new AgentContext();
            AgentInput input = new AgentInput();
            input.setContext(ctx);
            input.setUserMessage(prompt);

            AgentOutput output = genAgent.execute(input);
            if ("success".equals(output.getStatus())) {
                ResourceReport report = (ResourceReport) output.getStructuredData();
                report.setPathNodeId(node.getId());
                return report;
            }
        } catch (Exception e) {
            log.error("生成资源失败: node={}, type={}", node.getId(), type, e);
        }
        return null;
    }

    private void saveGeneratedResource(Long sessionId, PathNode node, ResourceReport report) {
        try {
            GeneratedResource resource = new GeneratedResource();
            resource.setPathNodeId(node.getId());
            resource.setTitle(report.getTitle());
            resource.setResourceType(report.getType());
            resource.setDifficulty(report.getDifficulty() != null ? report.getDifficulty() : "L1");
            resource.setGeneratedBySessionId(sessionId);

            Object contentObj = report.getContent();
            JsonNode contentNode = (contentObj instanceof String s)
                    ? objectMapper.readTree(s) : objectMapper.valueToTree(contentObj);
            resource.setContentJson(contentNode);

            generatedResourceService.batchCreateResources(List.of(resource));
        } catch (Exception e) {
            log.error("保存生成资源失败", e);
        }
    }

    // ==================== 辅助方法 ====================

    private AgentSession getOrCreateSession(Long sessionId, String message, Long userId) {
        if (sessionId == null) {
            AgentSession session = new AgentSession();
            session.setUserId(userId);
            session.setTitle(message.length() > 50 ? message.substring(0, 50) + "..." : message);
            session.setStatus("active");
            session.setCreatedAt(LocalDateTime.now());
            agentSessionMapper.insert(session);
            return session;
        }
        AgentSession session = agentSessionMapper.selectById(sessionId);
        if (session == null) throw new RuntimeException("会话不存在");
        return session;
    }

    private void saveUserMessage(Long sessionId, String message) {
        AgentMessage msg = new AgentMessage();
        msg.setSessionId(sessionId);
        msg.setRole("user");
        msg.setContent(message);
        msg.setCreatedAt(LocalDateTime.now());
        agentMessageMapper.insert(msg);
    }

    private String determinePhase(List<AgentMessage> history) {
        boolean hasDone = history.stream().anyMatch(m -> "done".equals(m.getPhase()));
        if (hasDone) return "done";
        boolean hasGenerating = history.stream().anyMatch(m -> "generating".equals(m.getPhase()));
        if (hasGenerating) return "done";
        boolean hasPlanning = history.stream().anyMatch(m -> "planning".equals(m.getPhase()));
        if (hasPlanning) return "generating";
        boolean profileComplete = history.stream()
                .filter(m -> "assistant".equals(m.getRole()))
                .anyMatch(m -> m.getContent() != null && m.getContent().contains("\"status\":\"complete\""));
        if (profileComplete) return "planning";
        return "profiling";
    }

    private void saveProfile(Long userId, JSONObject data, Long sessionId) {
        try {
            UserProfile profile = userProfileService.getUserProfile(userId);
            JsonNode dims = objectMapper.readTree(data.toJSONString());
            if (profile == null) {
                profile = new UserProfile();
                profile.setUserId(userId);
                profile.setDimensions(dims);
                profile.setUpdatedBySessionId(sessionId);
                userProfileService.createUserProfile(profile);
            } else {
                profile.setDimensions(dims);
                profile.setUpdatedBySessionId(sessionId);
                userProfileService.updateUserProfile(userId, profile);
            }
        } catch (Exception e) { log.error("保存画像失败", e); }
    }

    private void saveMessage(Long sessionId, String content, String phase) {
        AgentMessage msg = new AgentMessage();
        msg.setSessionId(sessionId);
        msg.setRole("assistant");
        msg.setContent(content);
        msg.setPhase(phase);
        msg.setCreatedAt(LocalDateTime.now());
        agentMessageMapper.insert(msg);
    }

    private void sendPhase(SseEmitter emitter, String phase, double progress, Object content, Long sessionId) throws IOException {
        Map<String, Object> data = new HashMap<>();
        data.put("phase", phase);
        data.put("progress", progress);
        if (sessionId != null) data.put("sessionId", sessionId);
        if (content != null) data.put("content", content);
        emitter.send(SseEmitter.event().name("phase").data(objectMapper.writeValueAsString(data)));
    }

    private void sendDone(SseEmitter emitter, Long sessionId) throws IOException {
        Map<String, Object> data = Map.of("phase", "done", "progress", 1.0, "sessionId", sessionId,
                "content", Map.of("profileSummary", Map.of("level", "已完成"), "pathPreview", "学习计划已生成"));
        emitter.send(SseEmitter.event().name("phase").data(objectMapper.writeValueAsString(data)));
    }

    private void sendError(SseEmitter emitter, String msg) {
        try { emitter.send(SseEmitter.event().name("error").data(Map.of("message", msg))); }
        catch (IOException e) { log.error("send error", e); }
    }

    private String extractJson(String text) {
        int s = text.indexOf("```json"), e = text.lastIndexOf("```");
        if (s != -1 && e > s) return text.substring(s + 7, e).trim();
        s = text.indexOf("{"); e = text.lastIndexOf("}");
        if (s != -1 && e > s) return text.substring(s, e + 1).trim();
        return text;
    }
}
