package com.exam.service;

import com.exam.entity.AgentMessage;
import com.exam.entity.AgentSession;

import java.util.List;

/**
 * Agent对话会话Service接口
 */
public interface AgentSessionService {

    /**
     * 获取用户对话历史列表
     */
    List<AgentSession> getSessionsByUser(Long userId);

    /**
     * 获取某次对话的消息列表
     */
    List<AgentMessage> getMessagesBySession(Long sessionId);

    /**
     * 创建会话
     */
    AgentSession createSession(AgentSession session);

    /**
     * 添加消息
     */
    void addMessage(AgentMessage message);

    /**
     * 更新会话状态
     */
    void updateSessionStatus(Long sessionId, String status);

    /**
     * 删除对话记录
     */
    void deleteSession(Long sessionId);

    /**
     * 根据会话ID获取会话详情
     */
    AgentSession getSessionById(Long sessionId);

    /**
     * 根据会话ID和阶段获取消息列表
     */
    List<AgentMessage> getMessagesBySessionAndPhase(Long sessionId, String phase);

    /**
     * 根据会话ID获取会话详情
     */
    AgentSession getSessionById(Long sessionId);
}
