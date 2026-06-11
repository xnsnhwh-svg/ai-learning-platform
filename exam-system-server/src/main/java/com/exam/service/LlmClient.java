package com.exam.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class LlmClient {

    @Value("${kimi.api.api-key:}")
    private String apiKey;

    @Value("${kimi.api.base-url:https://api.xiaomimimo.com/v1}")
    private String baseUrl;

    @Value("${kimi.api.model:mimo-v2.5-pro}")
    private String model;

    private final WebClient webClient = WebClient.builder()
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();

    /** 累计 token 消耗 */
    private final AtomicLong totalTokensUsed = new AtomicLong(0);

    public long getTotalTokensUsed() {
        return totalTokensUsed.get();
    }

    // ==================== 同步调用 ====================

    public String chat(String systemPrompt, List<Map<String, String>> messages) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("你的")) {
            throw new RuntimeException("未配置有效的 API 密钥，请在 application.yml 中设置 kimi.api.api-key");
        }

        JSONObject body = buildRequestBody(systemPrompt, messages);

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                String response = webClient.post()
                        .uri(baseUrl + "/chat/completions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                        .bodyValue(body.toJSONString())
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(120))
                        .block();

                JSONObject json = JSON.parseObject(response);
                if (json.containsKey("error")) {
                    String errMsg = json.getJSONObject("error").getString("message");
                    if (errMsg.contains("rate limit") && attempt < 3) {
                        Thread.sleep(attempt * 5000L);
                        continue;
                    }
                    throw new RuntimeException("LLM API 错误: " + errMsg);
                }

                // 记录 token 消耗
                recordTokenUsage(json);

                JSONArray choices = json.getJSONArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    return choices.getJSONObject(0).getJSONObject("message").getString("content");
                }
                throw new RuntimeException("LLM 返回空响应");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("调用被中断");
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                if (attempt == 3) throw new RuntimeException("LLM 服务不可用: " + e.getMessage());
                try { Thread.sleep(2000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
        throw new RuntimeException("LLM 调用失败");
    }

    // ==================== 流式调用（SSE） ====================

    public void chatStream(String systemPrompt, List<Map<String, String>> messages, SseEmitter emitter) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("你的")) {
            sendStreamError(emitter, "未配置有效的 API 密钥");
            return;
        }

        JSONObject body = buildRequestBody(systemPrompt, messages);
        body.put("stream", true);

        try {
            Flux<String> flux = webClient.post()
                    .uri(baseUrl + "/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(body.toJSONString())
                    .retrieve()
                    .bodyToFlux(String.class)
                    .timeout(Duration.ofSeconds(300));

            flux.doOnComplete(() -> {
                try {
                    emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                    emitter.complete();
                } catch (IOException e) { log.error("SSE complete error", e); }
            }).doOnError(e -> {
                log.error("SSE stream error", e);
                sendStreamError(emitter, "流式调用失败: " + e.getMessage());
            }).subscribe(chunk -> {
                try {
                    emitter.send(SseEmitter.event().name("token").data(chunk));
                } catch (IOException e) {
                    log.error("SSE send error", e);
                }
            });
        } catch (Exception e) {
            log.error("启动流式调用失败", e);
            sendStreamError(emitter, "流式调用启动失败: " + e.getMessage());
        }
    }

    // ==================== 私有方法 ====================

    private JSONObject buildRequestBody(String systemPrompt, List<Map<String, String>> messages) {
        JSONArray msgArray = new JSONArray();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JSONObject sys = new JSONObject();
            sys.put("role", "system");
            sys.put("content", systemPrompt);
            msgArray.add(sys);
        }
        for (Map<String, String> m : messages) {
            JSONObject msg = new JSONObject();
            msg.put("role", m.get("role"));
            msg.put("content", m.get("content"));
            msgArray.add(msg);
        }

        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("messages", msgArray);
        body.put("temperature", 0.7);
        body.put("max_tokens", 4000);
        return body;
    }

    private void recordTokenUsage(JSONObject response) {
        try {
            if (response.containsKey("usage")) {
                JSONObject usage = response.getJSONObject("usage");
                long tokens = usage.getLongValue("total_tokens");
                totalTokensUsed.addAndGet(tokens);
                log.info("Token 消耗: +{} (累计: {})", tokens, totalTokensUsed.get());
            }
        } catch (Exception e) {
            log.debug("记录 token 消耗失败", e);
        }
    }

    private void sendStreamError(SseEmitter emitter, String msg) {
        try {
            emitter.send(SseEmitter.event().name("error").data(Map.of("message", msg)));
            emitter.complete();
        } catch (IOException e) { log.error("send stream error", e); }
    }
}
