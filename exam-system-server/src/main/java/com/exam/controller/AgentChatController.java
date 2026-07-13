package com.exam.controller;

import com.exam.common.Result;
import com.exam.entity.AgentSession;
import com.exam.service.AgentSessionService;
import com.exam.service.agent.AgentOrchestrator;
import com.exam.service.agent.AgentOutput;
import com.exam.service.agent.Orchestrator;
import com.exam.service.agent.SSEFormatter;
import com.exam.service.impl.AgentOrchestratorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/agent")
@Tag(name = "Agent对话", description = "AI Agent对话接口")
public class AgentChatController {

    private static final Logger log = LoggerFactory.getLogger(AgentChatController.class);
    private static final ExecutorService executor = Executors.newFixedThreadPool(8);

    @Autowired
    private AgentOrchestratorService orchestratorService;

    @Autowired
    private AgentOrchestrator agentOrchestrator;

    @Autowired
    private Orchestrator orchestrator;

    @Autowired
    private AgentSessionService sessionService;

    @Autowired
    private SSEFormatter sseFormatter;

    @Value("${agent.mode:legacy}")
    private String agentMode;

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI对话（SSE流式）", description = "与AI Agent进行流式对话")
    public SseEmitter chat(@RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(300000L);

        Long sessionId = request.getSessionId();
        String message = request.getMessage();
        Long userId = request.getUserId() != null ? request.getUserId() : 1L;

        executor.execute(() -> {
            try {
                if ("legacy".equals(agentMode)) {
                    orchestratorService.handleChat(sessionId, message, userId, emitter);
                } else {
                    agentOrchestrator.handleChat(sessionId, message, userId, emitter);
                }
            } catch (Exception e) {
                log.error("Chat处理失败", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data("{\"message\":\"处理失败\"}"));
                } catch (Exception ex) {
                    log.error("SSE发送错误消息失败", ex);
                }
                emitter.complete();
            }
        });

        return emitter;
    }

    @PostMapping("/profile-from-diagnostic")
    @Operation(summary = "诊断问卷画像生成", description = "提交诊断问卷答案，通过agent链生成学习画像")
    public Result<?> profileFromDiagnostic(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.getOrDefault("userId", 1).toString());
        String diagnosticText = request.getOrDefault("diagnosticText", "").toString();

        if (diagnosticText.isBlank()) {
            return Result.error("诊断信息不能为空");
        }

        // 创建临时 agent session 并运行完整 agent 链
        AgentSession newSession = new AgentSession();
        newSession.setUserId(userId);
        newSession.setTitle("诊断画像生成 - " + diagnosticText.substring(0, Math.min(30, diagnosticText.length())));
        newSession.setStatus("active");
        AgentSession created = sessionService.createSession(newSession);

        try {
            List<AgentOutput> outputs = orchestrator.processMessage(userId, created.getId(), diagnosticText);
            return Result.success(Map.of(
                    "sessionId", created.getId(),
                    "outputs", outputs
            ));
        } catch (Exception e) {
            log.error("画像生成失败", e);
            return Result.error("画像生成失败: " + e.getMessage());
        }
    }

    @GetMapping("/status")
    @Operation(summary = "查询Agent会话状态", description = "查询用户活跃的Agent会话")
    public Result<?> getAgentStatus(@RequestParam Long userId) {
        List<AgentSession> sessions = sessionService.getSessionsByUser(userId);
        boolean hasActiveSession = sessions.stream().anyMatch(s -> "active".equals(s.getStatus()));
        return Result.success(Map.of(
                "hasActiveSession", hasActiveSession,
                "activeSessionCount", sessions.stream().filter(s -> "active".equals(s.getStatus())).count(),
                "totalSessions", sessions.size()
        ));
    }

    public static class ChatRequest {
        private Long sessionId;
        private String message;
        private Long userId;

        public Long getSessionId() { return sessionId; }
        public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
    }

    /**
     * 从诊断问卷创建画像 + 完整学习路径（同步，非SSE）
     * 前端问卷完成后调用此接口
     */
    @PostMapping("/profile-from-diagnostic")
    @Operation(summary = "问卷诊断建画像", description = "接受诊断问卷答案，创建画像后自动走完agent链")
    public Result<?> profileFromDiagnostic(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.getOrDefault("userId", 1).toString());
        String diagnosticText = request.getOrDefault("diagnosticText", "").toString();

        if (diagnosticText.isBlank()) {
            return Result.error("诊断信息不能为空");
        }

        try {
            // 创建新 session（以诊断文本摘要为标题）
            AgentSession newSession = new AgentSession();
            newSession.setUserId(userId);
            newSession.setTitle(diagnosticText.length() > 50
                    ? diagnosticText.substring(0, 50) + "..." : diagnosticText);
            newSession.setStatus("active");
            AgentSession created = sessionService.createSession(newSession);
            Long sessionId = created.getId();

            // 构建携带诊断文本的输入，直接走 Orchestrator 链
            // 把诊断文本作为 userMessage，Orchestrator 会走 ProfileAgent → PlanAgent → GenAgent
            List<AgentOutput> outputs = orchestrator.processMessage(userId, sessionId, diagnosticText);

            return Result.success(Map.of(
                    "sessionId", sessionId,
                    "outputs", outputs.stream().map(o -> Map.of(
                            "phase", o.getPhase(),
                            "message", o.getMessage(),
                            "completed", o.isCompleted(),
                            "data", o.getData(),
                            "suggestions", o.getSuggestions()
                    )).toList()
            ));
        } catch (Exception e) {
            return Result.error("处理诊断信息出错: " + e.getMessage());
        }
    }

    /**
     * 检查用户是否有活跃的画像和路径
     */
    @GetMapping("/status")
    @Operation(summary = "检查用户agent状态", description = "返回用户是否有活跃的agent session和路径")
    public Result<?> getAgentStatus(@RequestParam Long userId) {
        List<AgentSession> sessions = sessionService.getSessionsByUser(userId);
        boolean hasActiveSession = sessions.stream().anyMatch(s -> "active".equals(s.getStatus()));
        return Result.success(Map.of(
                "hasActiveSession", hasActiveSession,
                "activeSessionCount", sessions.stream().filter(s -> "active".equals(s.getStatus())).count(),
                "totalSessions", sessions.size()
        ));
    }
}
