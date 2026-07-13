package com.exam.service.agent;

import java.util.List;

public class AgentOutput {
    private final String message;
    private final String phase;
    private final boolean completed;
    private final Object data;
    private final List<String> suggestions;

    public AgentOutput(String message, String phase, boolean completed, Object data, List<String> suggestions) {
        this.message = message;
        this.phase = phase;
        this.completed = completed;
        this.data = data;
        this.suggestions = suggestions;
    }

    public static AgentOutput done(String nextPhase, String message, Object data) {
        return new AgentOutput(message, nextPhase, true, data, null);
    }

    public static AgentOutput done(String nextPhase, String message, Object data, List<String> suggestions) {
        return new AgentOutput(message, nextPhase, true, data, suggestions);
    }

    public static AgentOutput inProgress(String phase, String message) {
        return new AgentOutput(message, phase, false, null, null);
    }

    public String getMessage() { return message; }
    public String getPhase() { return phase; }
    public boolean isCompleted() { return completed; }
    public Object getData() { return data; }
    public List<String> getSuggestions() { return suggestions; }
}
