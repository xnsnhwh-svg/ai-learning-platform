package com.exam.controller;

import com.exam.common.Result;
import com.exam.entity.AgentMessage;
import com.exam.entity.AgentSession;
import com.exam.service.AgentSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Agent对话会话控制器
 * 处理Agent对话会话相关的HTTP请求
 */
@RestController
@RequestMapping("/api/agent")
@Tag(name = "Agent对话会话", description = "Agent对话会话相关操作，包括会话历史、消息查询和删除")
public class SessionController {

    @Autowired
    private AgentSessionService agentSessionService;

    @GetMapping("/sessions")
    @Operation(summary = "获取用户对话历史", description = "获取用户的对话历史列表")
    public Result<List<AgentSession>> getSessionsByUser(
            @Parameter(description = "用户ID") @RequestParam Long userId) {
        return Result.success(agentSessionService.getSessionsByUser(userId));
    }

    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "获取会话详情", description = "获取指定会话的详细信息")
    public Result<AgentSession> getSessionById(
            @Parameter(description = "会话ID") @PathVariable Long sessionId) {
        AgentSession session = agentSessionService.getSessionById(sessionId);
        if (session == null) {
            return Result.error("会话不存在");
        }
        return Result.success(session);
    }

    @GetMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "获取会话消息列表", description = "获取某次对话的消息列表")
    public Result<List<AgentMessage>> getMessagesBySession(
            @Parameter(description = "会话ID") @PathVariable Long sessionId) {
        return Result.success(agentSessionService.getMessagesBySession(sessionId));
    }

    @GetMapping("/sessions/{sessionId}/messages/phase/{phase}")
    @Operation(summary = "获取指定阶段消息", description = "获取某次对话中指定阶段的消息列表")
    public Result<List<AgentMessage>> getMessagesBySessionAndPhase(
            @Parameter(description = "会话ID") @PathVariable Long sessionId,
            @Parameter(description = "阶段") @PathVariable String phase) {
        return Result.success(agentSessionService.getMessagesBySessionAndPhase(sessionId, phase));
    }

    @PostMapping("/sessions")
    @Operation(summary = "创建会话", description = "创建新的对话会话")
    public Result<AgentSession> createSession(@RequestBody AgentSession session) {
        AgentSession created = agentSessionService.createSession(session);
        return Result.success(created);
    }

    @PostMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "添加消息", description = "向会话中添加新的消息")
    public Result<Void> addMessage(
            @Parameter(description = "会话ID") @PathVariable Long sessionId,
            @RequestBody AgentMessage message) {
        message.setSessionId(sessionId);
        agentSessionService.addMessage(message);
        return Result.success(null);
    }

    @PutMapping("/sessions/{sessionId}/status")
    @Operation(summary = "更新会话状态", description = "更新对话会话的状态")
    public Result<Void> updateSessionStatus(
            @Parameter(description = "会话ID") @PathVariable Long sessionId,
            @Parameter(description = "状态") @RequestParam String status) {
        agentSessionService.updateSessionStatus(sessionId, status);
        return Result.success(null);
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "删除对话记录", description = "删除指定的对话记录及其所有消息")
    public Result<Void> deleteSession(
            @Parameter(description = "会话ID") @PathVariable Long sessionId) {
        agentSessionService.deleteSession(sessionId);
        return Result.success(null);
    }
}
