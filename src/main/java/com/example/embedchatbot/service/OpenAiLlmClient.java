/*
 * OpenAI LLM 클라이언트 구현 파일.
 * - WebClient를 통해 /chat/completions REST API를 호출한다.
 * - API 키, 모델명, 조직 헤더 등은 환경변수/설정으로 주입된다.
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
 * <p>책임: HTTP 요청 생성, 429 재시도, 응답 파싱을 수행한다.</p>
 * <p>주의: WebClient는 동기 {@code block()} 호출을 사용하므로 스레드 풀 고갈에 유의한다.</p>
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

    /**
     * 구성 요소를 주입 받아 OpenAI 클라이언트를 초기화한다.
     * @param openAiWebClient {@link com.example.embedchatbot.config.LlmConfig}에서 제공한 WebClient
     * @param apiKey OpenAI API 키(환경변수/설정으로 주입)
     * @param model 사용할 Chat Completions 모델명
     */
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
     * OpenAI Chat Completions API를 호출해 텍스트를 생성한다.
     * <p>429(Too Many Requests) 재시도 전략:</p>
     * <pre>
     * attempt1: 약 400ms + Retry-After + ±100ms 지터
     * attempt2: 약 800ms + Retry-After + ±100ms 지터
     * attempt3: 약 1600ms + Retry-After + ±100ms 지터
     * (마지막 4번째 시도는 대기 없이 예외 전파 → ChatService에서 Echo 폴백 처리)
     * </pre>
     * @param systemPrompt 시스템 지침 메시지 (nullable)
     * @param userMessage 사용자 메시지
     * @return LLM이 생성한 응답 문자열
     * @throws Exception 네트워크 오류, 429 초과 재시도, quota 초과 등 복구 불가 상황
     */
    @Override
    public String generate(String systemPrompt, String userMessage) throws Exception {
        if (!enabled()) {
            throw new IllegalStateException("OPENAI_API_KEY not configured");
        }

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
                        .block();

                if (resp == null || resp.choices == null || resp.choices.isEmpty()
                        || resp.choices.get(0).message == null) {
                    throw new RuntimeException("Empty response from LLM");
                }
                return resp.choices.get(0).message.content;

            } catch (WebClientResponseException.TooManyRequests e) {
                // 429 처리: 쿼터부족이면 즉시 중단, 그 외에는 백오프 후 재시도
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

    /**
     * Retry-After 헤더 값을 밀리초 단위로 변환한다.
     * <p>숫자(초) 또는 RFC 7231 HTTP-date 포맷을 모두 지원한다.</p>
     * @param ra Retry-After 헤더 문자열
     * @return 대기해야 할 시간(ms). 파싱 실패 시 0
     */
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
