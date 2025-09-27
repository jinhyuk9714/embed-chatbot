package com.example.embedchatbot.llm;

import com.example.embedchatbot.chat.ChatUsage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class OpenAiChatClient implements ChatModelClient {

    private final WebClient webClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String apiKey;
    private final String model;
    private final double temperature;
    private final int maxTokens;

    public OpenAiChatClient(WebClient openAiWebClient,
                            @Value("${openai.api-key:}") String apiKey,
                            @Value("${openai.model:gpt-4o-mini}") String model,
                            @Value("${openai.temperature:0.3}") double temperature,
                            @Value("${openai.max-tokens:512}") int maxTokens) {
        this.webClient = openAiWebClient;
        this.apiKey = apiKey;
        this.model = model;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
    }

    @Override
    public boolean isEnabled() {
        return StringUtils.hasText(apiKey);
    }

    @Override
    public void streamChat(List<Map<String, String>> messages, String sessionId, StreamHandler handler) {
        if (!isEnabled()) {
            handler.onError(new IllegalStateException("OPENAI_API_KEY missing"));
            return;
        }

        Map<String, Object> payload = Map.of(
                "model", model,
                "messages", messages,
                "temperature", temperature,
                "max_tokens", maxTokens,
                "stream", true
        );

        AtomicBoolean completed = new AtomicBoolean(false);

        webClient.post()
                .uri("/chat/completions")
                .headers(h -> h.setBearerAuth(apiKey))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(payload)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofSeconds(90))
                .onErrorResume(error -> {
                    handler.onError(error);
                    return Flux.empty();
                })
                .flatMap(body -> Flux.fromArray(body.split("\n")))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .subscribe(line -> handleLine(line, handler, completed),
                        handler::onError,
                        () -> {
                            if (completed.compareAndSet(false, true)) {
                                handler.onComplete();
                            }
                        });
    }

    private void handleLine(String line, StreamHandler handler, AtomicBoolean completed) {
        if (!line.startsWith("data:")) {
            return;
        }
        String data = line.substring(5).trim();
        if (data.isEmpty()) {
            return;
        }
        if ("[DONE]".equals(data)) {
            if (completed.compareAndSet(false, true)) {
                handler.onComplete();
            }
            return;
        }
        try {
            JsonNode root = mapper.readTree(data);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && choices.size() > 0) {
                JsonNode delta = choices.get(0).path("delta");
                if (delta.hasNonNull("content")) {
                    handler.onToken(delta.get("content").asText());
                }
            }
            JsonNode usage = root.path("usage");
            if (usage.isObject()) {
                ChatUsage chatUsage = new ChatUsage(
                        usage.path("prompt_tokens").asInt(0),
                        usage.path("completion_tokens").asInt(0),
                        0L,
                        UUID.randomUUID().toString(),
                        0
                );
                handler.onUsage(chatUsage);
            }
        } catch (Exception ex) {
            handler.onError(ex);
        }
    }
}
