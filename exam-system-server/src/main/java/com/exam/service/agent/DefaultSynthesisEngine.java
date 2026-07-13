package com.exam.service.agent;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DefaultSynthesisEngine implements SynthesisEngine {

    @Override
    public SynthesisResult synthesize(List<AgentOutput> agentOutputs) {
        StringBuilder summary = new StringBuilder();
        int completedCount = 0;
        for (AgentOutput output : agentOutputs) {
            summary.append("【阶段：").append(output.getPhase()).append("】\n");
            summary.append(output.getMessage()).append("\n\n");
            if (output.isCompleted()) completedCount++;
        }
        return new SynthesisResult(summary.toString().trim(), completedCount == agentOutputs.size());
    }
}
