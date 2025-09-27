// File: src/main/java/com/example/embedchatbot/service/OpenAiLlmClient.java
package com.example.embedchatbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.embedchatbot.dto.ChatUsage;
import com.example.embedchatbot.llm.LlmClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class OpenAiLlmClient implements LlmClient {

    private final WebClient client;
    private final ObjectMapper om = new ObjectMapper();

    private final String apiKey;
    private final String model;
    private final Double temperature;
    private final Integer maxTokens;

    public OpenAiLlmClient(
            WebClient openAiWebClient,
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.model:gpt-4o-mini}") String model,
            @Value("${openai.temperature:0.2}") Double temperature,
            @Value("${openai.max-tokens:512}") Integer maxTokens
    ) {
        this.client = openAiWebClient;
        this.apiKey = apiKey;
        this.model = model;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
    }

    public boolean isEnabled() { return StringUtils.hasText(apiKey); }

    @Override
    public void streamChat(List<Map<String, String>> messages, String sessionId, Callback cb) {
        if (!isEnabled()) {
            cb.onError("OPENAI_API_KEY missing");
            return;
        }

        Map<String, Object> payload = Map.of(
                "model", model,
                "messages", messages,
                "temperature", temperature,
                "max_tokens", maxTokens,
                "stream", true
        );

        AtomicBoolean done = new AtomicBoolean(false);

        client.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .headers(h -> h.setBearerAuth(apiKey))
                .bodyValue(payload)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofSeconds(90))
                .onErrorResume(e -> {
                    cb.onError(e.getMessage() == null ? "openai_error" : e.getMessage());
                    return Flux.empty();
                })
                // ✅ bodyToFlux(String) → Flux<String>; 줄 단위로 펼칠 땐 flatMap 사용
                .flatMap(s -> Flux.fromArray(s.split("\n")))
                .map(String::trim)
                .filter(l -> !l.isEmpty())
                .subscribe(line -> {
                    if (!line.startsWith("data:")) return;
                    String data = line.substring(5).trim();

                    if ("[DONE]".equals(data)) {
                        if (done.compareAndSet(false, true)) {
                            // usage 추정이 없을 수 있으므로 최소 usage 전송 후 done
                            cb.onUsage(new ChatUsage(0, 0, 0L, UUID.randomUUID().toString(), 0));
                            cb.onDone();
                        }
                        return;
                    }
                    try {
                        JsonNode n = om.readTree(data);
                        JsonNode choices = n.path("choices");
                        if (choices.isArray() && choices.size() > 0) {
                            JsonNode delta = choices.get(0).path("delta");
                            String token = delta.path("content").asText(null);
                            if (token != null) cb.onToken(token);
                        }
                    } catch (Exception ex) {
                        cb.onError("parse_error");
                    }
                }, err -> {
                    cb.onError(err.getMessage() == null ? "openai_stream_error" : err.getMessage());
                }, () -> {
                    // 스트림이 정상 종료되었지만 [DONE]을 못 본 경우 방어적 완료 처리
                    if (done.compareAndSet(false, true)) {
                        cb.onUsage(new ChatUsage(0, 0, 0L, UUID.randomUUID().toString(), 0));
                        cb.onDone();
                    }
                });
    }
}
