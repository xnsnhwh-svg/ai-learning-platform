package com.exam.service.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Service
public class DeepSeekLLMClient implements LLMClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${deepseek.api.base-url}")
    private String baseUrl;

    @Value("${deepseek.api.api-key}")
    private String apiKey;

    @Value("${deepseek.api.model}")
    private String model;

    @Value("${deepseek.api.max-tokens}")
    private int maxTokens;

    @Value("${deepseek.api.temperature}")
    private double temperature;

    public DeepSeekLLMClient(ObjectMapper objectMapper) {
        this.webClient = WebClient.builder().build();
        this.objectMapper = objectMapper;
    }

    @Override
    public String chat(List<Map<String, String>> messages) {
        try {
            String response = webClient.post()
                    .uri(baseUrl + "/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(buildRequestBody(messages, false))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return LLMResponseParser.parseChatResponse(objectMapper, response);
        } catch (Exception e) {
            throw new RuntimeException("LLM call failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Flux<String> chatStream(List<Map<String, String>> messages) {
        return webClient.post()
                .uri(baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(buildRequestBody(messages, true))
                .retrieve()
                .bodyToFlux(String.class)
                .transform(LLMResponseParser::parseStreamResponse);
    }

    private String buildRequestBody(List<Map<String, String>> messages, boolean stream) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        body.put("temperature", temperature);
        body.put("stream", stream);

        ArrayNode messagesArray = body.putArray("messages");
        for (Map<String, String> msg : messages) {
            ObjectNode msgNode = messagesArray.addObject();
            msgNode.put("role", msg.get("role"));
            msgNode.put("content", msg.get("content"));
        }
        return body.toString();
    }
}
