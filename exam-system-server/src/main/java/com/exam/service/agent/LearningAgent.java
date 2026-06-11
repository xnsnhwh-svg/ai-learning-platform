package com.exam.service.agent;

import com.exam.service.agent.model.AgentContext;
import com.exam.service.agent.model.AgentInput;
import com.exam.service.agent.model.AgentOutput;

/**
 * 所有 Agent 的统一接口
 */
public interface LearningAgent {
    String getRole();
    String buildSystemPrompt(AgentContext ctx);
    AgentOutput execute(AgentInput input);
}
