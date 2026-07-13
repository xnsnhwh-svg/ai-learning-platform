package com.exam.service.agent;

import com.exam.service.agent.model.AgentContext;
import com.exam.service.agent.model.AgentInput;
import com.exam.service.agent.model.AgentOutput;

/**
 * 瀛︿範Agent缁熶竴鎺ュ彛
 * 鎵€鏈堿gent锛圥rofileAgent銆丳lanAgent銆丟enAgent锛夐兘瀹炵幇姝ゆ帴鍙? */
public interface LearningAgent {

    /**
     * 鑾峰彇Agent瑙掕壊鍚?     * @return 濡?"ProfileAgent", "PlanAgent", "GenAgent"
     */
    String getRole();

    /**
     * 鍔ㄦ€佹瀯寤簊ystem prompt
     * @param ctx Agent涓婁笅鏂?     * @return system prompt瀛楃涓?     */
    String buildSystemPrompt(AgentContext ctx);

    /**
     * 鎵цAgent浠诲姟
     * @param input Agent杈撳叆
     * @return Agent杈撳嚭
     */
    AgentOutput execute(AgentInput input);
}
