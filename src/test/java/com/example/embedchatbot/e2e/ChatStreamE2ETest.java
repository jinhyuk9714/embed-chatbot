package com.example.embedchatbot.e2e;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Why: /v1/chat/stream SSE에서 token/usage/done이 짧은 시간 내 수신되는지 검증.
 * Note: EventSource 포맷은 "event:"/ "data:" 라인으로 오므로, 문자열 라인에서 식별.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = {
        "server.port=9000",
        "stream.delayMs=1",
        "stream.heartbeatMs=5000"
})
public class ChatStreamE2ETest {

    @Value("${CHAT_API_KEY:dev-key}")
    String apiKey;

    @Test
    void sse_flow_should_emit_token_usage_done() {
        WebClient client = WebClient.builder()
                .baseUrl("http://localhost:9000")
                .defaultHeader("Accept", "text/event-stream")
                .defaultHeader("X-API-Key", apiKey)
                .build();

        Flux<String> body = client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/chat/stream")
                        .queryParam("botId", "test-bot")
                        .queryParam("message", "hello-test")
                        .build())
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofSeconds(10));

        AtomicInteger tokenCount = new AtomicInteger();
        AtomicBoolean usageSeen = new AtomicBoolean(false);
        AtomicBoolean doneSeen = new AtomicBoolean(false);

        StepVerifier.create(body.doOnNext(line -> {
                    if (line.contains("event: token")) tokenCount.incrementAndGet();
                    if (line.contains("event: usage")) usageSeen.set(true);
                    if (line.contains("event: done")) doneSeen.set(true);
                }))
                .thenAwait(Duration.ofSeconds(3))
                .thenCancel()
                .verify();

        // 단언: 최소 한 개 이상의 token, usage/done 이벤트가 관측되었다고 간주
        assert tokenCount.get() > 0 : "no token events";
        assert usageSeen.get() : "no usage event";
        assert doneSeen.get() : "no done event";
    }
}