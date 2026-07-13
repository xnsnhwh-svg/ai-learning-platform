package com.exam.service.agent.prompt;

import java.util.List;
import java.util.Map;

public interface PromptBuilder {
    List<Map<String, String>> build(String userMessage, List<Map<String, String>> history);
    String getPhase();
}
