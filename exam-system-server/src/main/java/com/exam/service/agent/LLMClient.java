package com.exam.service.agent;

import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

public interface LLMClient {
    String chat(List<Map<String, String>> messages);
    Flux<String> chatStream(List<Map<String, String>> messages);
}
