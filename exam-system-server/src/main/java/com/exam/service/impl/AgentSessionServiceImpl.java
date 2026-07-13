package com.exam.service.impl;

import com.exam.entity.AgentMessage;
import com.exam.entity.AgentSession;
import com.exam.mapper.AgentMessageMapper;
import com.exam.mapper.AgentSessionMapper;
import com.exam.service.AgentSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Agent对话会话Service实现类
 */
@Service
public class AgentSessionServiceImpl implements AgentSessionService {

    @Autowired
    private AgentSessionMapper agentSessionMapper;

    @Autowired
    private AgentMessageMapper agentMessageMapper;

    @Override
    public List<AgentSession> getSessionsByUser(Long userId) {
        return agentSessionMapper.selectByUserId(userId);
    }

    @Override
    public List<AgentMessage> getMessagesBySession(Long sessionId) {
        return agentMessageMapper.selectBySessionId(sessionId);
    }

    @Override
    @Transactional
    public AgentSession createSession(AgentSession session) {
        agentSessionMapper.insert(session);
        return session;
    }

    @Override
    @Transactional
    public void addMessage(AgentMessage message) {
        agentMessageMapper.insert(message);
    }

    @Override
    @Transactional
    public void updateSessionStatus(Long sessionId, String status) {
        AgentSession session = agentSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new RuntimeException("会话不存在");
        }
        session.setStatus(status);
        agentSessionMapper.updateById(session);
    }

    @Override
    @Transactional
    public void deleteSession(Long sessionId) {
        // 删除消息
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AgentMessage> wrapper = 
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(AgentMessage::getSessionId, sessionId);
        agentMessageMapper.delete(wrapper);
        // 删除会话
        agentSessionMapper.deleteById(sessionId);
    }

    @Override
    public AgentSession getSessionById(Long sessionId) {
        AgentSession session = agentSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new RuntimeException("会话不存在");
        }
        return session;
    }

    @Override
    public List<AgentMessage> getMessagesBySessionAndPhase(Long sessionId, String phase) {
        return agentMessageMapper.selectBySessionIdAndPhase(sessionId, phase);
    }

    @Override
    public AgentSession getSessionById(Long sessionId) {
        return agentSessionMapper.selectById(sessionId);
    }
}
