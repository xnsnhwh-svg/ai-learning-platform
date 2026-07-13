package com.exam.service.agent;

import com.exam.entity.AgentMessage;
import com.exam.entity.AgentSession;
import com.exam.service.AgentSessionService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SessionManager {

    private final AgentSessionService sessionService;

    public SessionManager(AgentSessionService sessionService) {
        this.sessionService = sessionService;
    }

    public AgentSession getOrCreateSession(Long userId, Long sessionId, String message) {
        AgentSession session = null;
        if (sessionId != null) {
            Long searchId = sessionId;
            session = sessionService.getSessionsByUser(userId).stream()
                    .filter(s -> s.getId().equals(searchId))
                    .findFirst().orElse(null);
        }
        if (session == null) {
            AgentSession newSession = new AgentSession();
            newSession.setUserId(userId);
            newSession.setTitle(message.length() > 50 ? message.substring(0, 50) + "..." : message);
            newSession.setStatus("active");
            session = sessionService.createSession(newSession);
        }
        return session;
    }

    public void saveUserMessage(Long sessionId, String content) {
        AgentMessage msg = new AgentMessage();
        msg.setSessionId(sessionId);
        msg.setRole("user");
        msg.setContent(content);
        sessionService.addMessage(msg);
    }

    public void saveAssistantMessage(Long sessionId, String content, String phase) {
        AgentMessage msg = new AgentMessage();
        msg.setSessionId(sessionId);
        msg.setRole("assistant");
        msg.setContent(content);
        msg.setPhase(phase);
        sessionService.addMessage(msg);
    }

    public void completeSession(Long sessionId) {
        sessionService.updateSessionStatus(sessionId, "completed");
    }

    public String determineCurrentPhase(Long sessionId) {
        List<AgentMessage> allMessages = sessionService.getMessagesBySession(sessionId);
        return allMessages.stream()
                .filter(m -> "assistant".equals(m.getRole()) && m.getPhase() != null)
                .reduce((first, second) -> second)
                .map(AgentMessage::getPhase)
                .map(this::nextPhase)
                .orElse("profiling");
    }

    public List<Map<String, String>> getConversationHistory(Long sessionId) {
        return sessionService.getMessagesBySession(sessionId).stream()
                .filter(m -> !"system".equals(m.getRole()))
                .map(m -> Map.of("role", m.getRole(), "content", m.getContent()))
                .collect(Collectors.toList());
    }

    private String nextPhase(String current) {
        return switch (current) {
            case "profiling" -> "planning";
            case "planning" -> "generating";
            case "generating" -> "done";
            default -> "profiling";
        };
    }
}
