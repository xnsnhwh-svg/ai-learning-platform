package com.exam.service.agent;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PhaseRouter {

    private final Map<String, LearningAgent> agentMap;

    public PhaseRouter(ProfileAgent profileAgent, PlanAgent planAgent, GenAgent genAgent) {
        agentMap = new ConcurrentHashMap<>();
        agentMap.put("profiling", profileAgent);
        agentMap.put("planning", planAgent);
        agentMap.put("generating", genAgent);
    }

    public LearningAgent resolve(String phase) {
        LearningAgent agent = agentMap.get(phase);
        if (agent == null) {
            agent = agentMap.get("profiling");
        }
        return agent;
    }
}
