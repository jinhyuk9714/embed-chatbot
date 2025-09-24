/*
 * 대화 비즈니스 로직을 담당하는 서비스.
 * 세션 ID 생성, LLM 호출/폴백, 토큰 사용량 추정 등 컨트롤러와 LLM 클라이언트 사이의 오케스트레이션을 수행한다.
 * 장애 시에도 응답성을 유지하기 위해 Echo 폴백 전략을 내장한다.
 */
package com.example.embedchatbot.service;

import com.example.embedchatbot.dto.ChatRequest;
import com.example.embedchatbot.dto.ChatResult;
import com.example.embedchatbot.dto.ChatUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * 대화 플로우를 총괄하는 서비스 레이어.
 * <p>세션 ID를 보정하고, LlmClient를 호출해 응답을 생성하며, 실패 시 Echo로 폴백하고 토큰 사용량을 추정한다.</p>
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final LlmClient llmClient;

    public ChatService(LlmClient llmClient) {
        this.llmClient = llmClient;
        // ⬇️ 앱 기동 시, 키 감지 여부를 1회 로깅(민감정보 노출 금지!)
        log.info("LLM enabled at startup: {}", llmClient != null && llmClient.enabled());
    }

    /**
     * LLM 대화 요청을 처리한다.
     * @param request 클라이언트에서 전달된 필수/선택 파라미터
     * @return LLM 또는 Echo 폴백으로 생성된 답변과 세션/토큰 정보를 담은 결과 DTO
     * @throws RuntimeException 하위 LlmClient에서 발생한 예외는 잡아 폴백하지만, 기타 예외는 로깅 후 다시 던질 수 있다.
     */
    public ChatResult chat(ChatRequest request) {
        final long t0 = System.nanoTime();
        log.debug("chat() called: botId={}, hasSession={}, msgLen={}",
                request.getBotId(),
                StringUtils.hasText(request.getSessionId()),
                request.getMessage() == null ? 0 : request.getMessage().length());

        String sessionId = resolveSessionId(request.getSessionId());
        String answer;

        try {
            if (llmClient != null && llmClient.enabled()) {
                log.debug("Using LLM for response (sessionId={})", sessionId);
                String sys = "You are a helpful assistant.";
                answer = llmClient.generate(sys, request.getMessage());
            } else {
                log.debug("LLM disabled → using Echo fallback (sessionId={})", sessionId);
                // LLM 비활성 시 가용성을 위해 단순 Echo 응답으로 즉시 폴백
                answer = buildEcho(request.getMessage());
            }
        } catch (Exception ex) {
            // LLM 실패(네트워크/쿼터/예외 등) 시에도 Echo로 폴백해 응답성을 확보
            log.warn("LLM call failed → Echo fallback. reason={}", ex.toString());
            answer = buildEcho(request.getMessage());
        }

        long latencyMs = (System.nanoTime() - t0) / 1_000_000;
        // 문자열 길이를 기반으로 한 간이 토큰 추정치 (정확한 토크나이저 아님)
        ChatUsage usage = new ChatUsage(estimateTokens(request.getMessage()), estimateTokens(answer));

        log.debug("chat() done: sessionId={}, latencyMs={}, promptTok={}, completionTok={}",
                sessionId, latencyMs, usage.promptTokens(), usage.completionTokens());

        return new ChatResult(answer, sessionId, usage);
    }

    private String resolveSessionId(String sessionId) {
        return StringUtils.hasText(sessionId) ? sessionId : UUID.randomUUID().toString();
    }

    private String buildEcho(String message) {
        String m = (message == null) ? "" : message.trim();
        return "Echo: " + m;
    }

    private int estimateTokens(String text) {
        if (!StringUtils.hasText(text)) return 0;
        return text.codePointCount(0, text.length());
    }
}
