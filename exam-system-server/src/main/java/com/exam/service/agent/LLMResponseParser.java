package com.exam.service.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Flux;

public class LLMResponseParser {

    private LLMResponseParser() {}

    public static String parseChatResponse(ObjectMapper objectMapper, String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse LLM response", e);
        }
    }

    public static Flux<String> parseStreamResponse(Flux<String> rawStream) {
        return rawStream
                .filter(data -> data.startsWith("data: "))
                .map(data -> data.substring(6))
                .filter(data -> !"[DONE]".equals(data.trim()))
                .map(data -> {
                    try {
                        JsonNode node = new ObjectMapper().readTree(data);
                        return node.path("choices").get(0).path("delta").path("content").asText("");
                    } catch (Exception e) {
                        return "";
                    }
                })
                .filter(s -> !s.isEmpty());
    }

    public static String extractTaggedContent(String text, String openTag, String closeTag) {
        int start = text.indexOf(openTag);
        if (start == -1) return null;
        int end = text.indexOf(closeTag, start + openTag.length());
        if (end == -1) return null;
        return text.substring(start + openTag.length(), end).trim();
    }

    public static String extractJsonBlock(String text) {
        int start = text.indexOf("```json");
        if (start == -1) return null;
        start += 7;
        int end = text.indexOf("```", start);
        if (end == -1) return null;
        return text.substring(start, end).trim();
    }

    public static String stripTag(String text, String tag) {
        return text.replace(tag, "").trim();
    }
}
