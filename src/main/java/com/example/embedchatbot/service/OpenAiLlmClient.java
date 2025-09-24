/*
 * OpenAI 호환 WebClient 기반 LLM 클라이언트 구현체.
 * 환경 변수로 주입된 모델/키/조직 정보와 Reactor WebClient를 사용해 동기 응답을 생성한다.
 * 타임아웃, Retry-After 파싱, 429 재시도 정책을 포함하며, 실패 시 상위 서비스가 Echo 폴백을 실행한다.
 */
package com.example.embedchatbot.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * OpenAI Chat Completions API를 호출하는 기본 구현체.
 * <p>애플리케이션 시작 시 주입된 WebClient를 사용하며, API 키 미설정 시 enabled()가 false를 반환한다.
 * 429 응답 시 백오프 + Retry-After를 조합해 재시도하고, 최종 실패는 상위 서비스가 Echo 폴백으로 처리한다.</p>
 */
@Component
public class OpenAiLlmClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiLlmClient.class);

    private final WebClient webClient;
    private final String apiKey;
    private final String model;

    // (옵션) 필요 시 설정하면 자동으로 헤더 추가됨
    @Value("${OPENAI_ORG:}")
    private String org;
    @Value("${OPENAI_PROJECT:}")
    private String project;

    public OpenAiLlmClient(
            WebClient openAiWebClient,
            @Value("${app.llm.api-key:}") String apiKey,
            @Value("${app.llm.model:gpt-4o-mini}") String model) {
        this.webClient = openAiWebClient;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public boolean enabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * OpenAI Chat Completions API를 호출해 응답을 생성한다.
     * <p>재시도 정책 요약:</p>
     * <ul>
     *     <li>attempt1: 250ms 기본 대기(현재 구현은 400ms 기반 백오프 + ±100ms 지터 적용)</li>
     *     <li>attempt2: 500ms 기본 대기(지터/Retry-After에 따라 추가 지연)</li>
     *     <li>attempt3: 1000ms 기본 대기(서버 Retry-After가 있으면 더 대기)</li>
     * </ul>
     * <p>각 단계는 Retry-After 헤더(초)를 추가로 고려하며, 마지막 시도 실패 시 예외를 전파해
     * 상위(ChatService)가 Echo 폴백을 수행한다.</p>
     */
    @Override
    public String generate(String systemPrompt, String userMessage) throws Exception {
        if (!enabled()) {
            throw new IllegalStateException("OPENAI_API_KEY not configured");
        }

        // WebClient 요청 본문 구성: system/user 메시지를 모델과 함께 전달
        var req = new ChatCompletionsRequest(
                model,
                List.of(
                        new Message("system", systemPrompt == null ? "" : systemPrompt),
                        new Message("user",   userMessage  == null ? "" : userMessage)
                )
        );

        final int maxRetry = 4;           // 4회 시도
        final long baseBackoffMs = 400L;  // 400 → 800 → 1600 → 3200ms

        for (int attempt = 1; attempt <= maxRetry; attempt++) {
            try {
                // POST /chat/completions 호출 시 인증/조직 헤더를 주입하고 JSON 요청을 전달
                ChatCompletionsResponse resp = webClient.post()
                        .uri("/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .headers(h -> {
                            h.setBearerAuth(apiKey);
                            if (org != null && !org.isBlank()) h.add("OpenAI-Organization", org);
                            if (project != null && !project.isBlank()) h.add("OpenAI-Project", project);
                        })
                        .bodyValue(req)
                        .retrieve()
                        .bodyToMono(ChatCompletionsResponse.class)
                        // 동기 흐름(ChatService)과 맞추기 위해 block() 사용 (WebFlux 비동기 전파 불필요)
                        .block();

                if (resp == null || resp.choices == null || resp.choices.isEmpty()
                        || resp.choices.get(0).message == null) {
                    throw new RuntimeException("Empty response from LLM");
                }
                return resp.choices.get(0).message.content;

            } catch (WebClientResponseException.TooManyRequests e) {
                // 429 처리: 쿼터 부족이면 즉시 상위로 전달, 그 외에는 백오프 후 재시도
                String body = e.getResponseBodyAsString();
                boolean quota = body != null && body.contains("insufficient_quota");
                if (quota) {
                    log.warn("OpenAI quota exceeded (insufficient_quota).");
                    throw new LlmQuotaExceededException("OpenAI quota exceeded");
                }

                // Retry-After: 초 또는 HTTP-date 모두 지원
                String ra = e.getHeaders() != null ? e.getHeaders().getFirst("Retry-After") : null;
                long retryAfterMs = parseRetryAfterToMillis(ra);

                if (attempt == maxRetry) throw e;

                long backoffMs = (baseBackoffMs * (1L << (attempt - 1))) + retryAfterMs;
                long jitter = ThreadLocalRandom.current().nextLong(-100, 101); // -100~+100ms
                long sleepMs = Math.max(0, backoffMs + jitter);

                log.debug("429 retry attempt {}/{} → sleep {}ms (Retry-After {}ms, backoff {}ms)",
                        attempt, maxRetry, sleepMs, retryAfterMs, backoffMs);

                try { Thread.sleep(sleepMs); } catch (InterruptedException ignored) {}
            }
        }

        throw new IllegalStateException("unreachable");
    }

    // --- minimal DTOs for /v1/chat/completions ---
    public static class ChatCompletionsRequest {
        public String model;
        public List<Message> messages;
        public ChatCompletionsRequest(String model, List<Message> messages) {
            this.model = model; this.messages = messages;
        }
    }
    public static class Message {
        public String role;
        public String content;
        public Message(String role, String content) { this.role = role; this.content = content; }
    }
    public static class ChatCompletionsResponse {
        public List<Choice> choices;
    }
    public static class Choice {
        public Message message;
        @JsonProperty("finish_reason")
        public String finishReason;
    }

    /** Retry-After 헤더를 ms로 변환 (초 or HTTP-date) */
    private long parseRetryAfterToMillis(String ra) {
        if (ra == null || ra.isBlank()) return 0L;
        if (ra.matches("\\d+")) {
            try { return Long.parseLong(ra) * 1000L; } catch (NumberFormatException ignore) {}
        }
        try {
            ZonedDateTime when = ZonedDateTime.parse(ra, DateTimeFormatter.RFC_1123_DATE_TIME);
            long diffMs = when.toInstant().toEpochMilli()
                    - ZonedDateTime.now(ZoneOffset.UTC).toInstant().toEpochMilli();
            return Math.max(0, diffMs);
        } catch (DateTimeParseException ignore) {
            return 0L;
        }
    }
}
