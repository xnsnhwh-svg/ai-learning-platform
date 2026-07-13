package com.exam.service.agent;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SSEFormatter {

    public String sessionEvent(Long sessionId, String title) {
        return toJson(Map.of("type", "session", "sessionId", sessionId, "title", title));
    }

    public String messageEvent(String content, String phase, boolean completed) {
        return toJson(Map.of("type", "message", "content", content,
                "phase", phase != null ? phase : "", "completed", completed));
    }

    public String dataEvent(String phase, Object data) {
        return toJson(Map.of("type", "data", "phase", phase, "data", data));
    }

    public String errorEvent(String error) {
        return toJson(Map.of("type", "error", "content", error));
    }

    public String suggestionsEvent(List<String> suggestions) {
        return toJson(Map.of("type", "suggestions", "suggestions", suggestions));
    }

    public String doneEvent() {
        return "[DONE]";
    }

    private String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(entry.getKey()).append("\":");
            Object val = entry.getValue();
            if (val instanceof String s) {
                sb.append("\"").append(escapeJson(s)).append("\"");
            } else if (val instanceof Number || val instanceof Boolean) {
                sb.append(val);
            } else if (val instanceof Map || val instanceof List) {
                sb.append(val);
            } else if (val == null) {
                sb.append("null");
            } else {
                sb.append("\"").append(escapeJson(val.toString())).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
