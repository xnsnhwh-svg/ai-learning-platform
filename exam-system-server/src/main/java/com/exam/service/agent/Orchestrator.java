package com.exam.service.agent;

import com.exam.entity.AgentSession;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class Orchestrator {

    private final SessionManager sessionManager;
    private final PhaseRouter phaseRouter;

    public Orchestrator(SessionManager sessionManager, PhaseRouter phaseRouter) {
        this.sessionManager = sessionManager;
        this.phaseRouter = phaseRouter;
    }

    public List<AgentOutput> processMessage(Long userId, Long sessionId, String message) {
        AgentSession session = sessionManager.getOrCreateSession(userId, sessionId, message);
        Long activeSessionId = session.getId();

        sessionManager.saveUserMessage(activeSessionId, message);

        String phase = sessionManager.determineCurrentPhase(activeSessionId);
        List<AgentOutput> outputs = new java.util.ArrayList<>();

        // 链式执行：一个 Agent 完成后立即执行下一个
        while (phase != null && !"done".equals(phase)) {
            LearningAgent agent = phaseRouter.resolve(phase);
            List<Map<String, String>> history = sessionManager.getConversationHistory(activeSessionId);
            AgentInput input = new AgentInput(userId, activeSessionId, message, history);

            AgentOutput output = agent.execute(input);
            outputs.add(output);

            // 以当前阶段（而非下一阶段）保存，使 determineCurrentPhase 的 nextPhase 计算正确
            sessionManager.saveAssistantMessage(activeSessionId, output.getMessage(), phase);

            if (output.isCompleted()) {
                phase = nextPhase(phase);
            } else {
                break;
            }
        }

        if ("done".equals(phase)) {
            sessionManager.completeSession(activeSessionId);
        }

        return outputs.isEmpty()
                ? java.util.List.of(AgentOutput.inProgress("profiling", "请描述你的学习目标"))
                : outputs;
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
