/*
 * 채팅 비즈니스 로직 서비스 파일.
 * - 세션 ID 결정, LLM 호출, 폴백(Echo) 처리, 토큰 사용량 추정을 담당한다.
 * - 외부 LLM 장애 시 가용성을 유지하는 전략을 명시한다.
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
 * 채팅 요청을 처리하는 서비스 계층.
 * <p>무엇: 세션 식별자 관리, LLM 호출, 폴백, 토큰 추정까지 End-to-End 흐름을 담당.</p>
 * <p>왜: 컨트롤러에서 비즈니스 로직을 분리하고 장애 대응 정책을 캡슐화한다.</p>
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final LlmClient llmClient;

    /**
     * LLM 클라이언트를 주입 받아 서비스 인스턴스를 생성한다.
     * @param llmClient 동기 LLM 호출을 담당하는 구현체
     */
    public ChatService(LlmClient llmClient) {
        this.llmClient = llmClient;
        // ⬇️ 앱 기동 시, 키 감지 여부를 1회 로깅(민감정보 노출 금지!)
        log.info("LLM enabled at startup: {}", llmClient != null && llmClient.enabled());
    }

    /**
     * 채팅 요청을 처리해 LLM 결과 또는 폴백 응답을 반환한다.
     * <p>흐름: 세션 ID 확정 → LLM 호출 → 실패 시 Echo 폴백 → 토큰 추정 → ChatResult 생성.</p>
     * @param request 클라이언트가 보낸 채팅 요청 DTO
     * @return LLM 또는 폴백 응답, 세션ID, 토큰 추정치를 담은 결과
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
                // LLM 비활성화(키 미설정) 시 즉시 Echo 폴백으로 가용성을 유지
                answer = buildEcho(request.getMessage());
            }
        } catch (Exception ex) {
            // LLM 호출 실패(네트워크/쿼터/429 등) 시 가용성 우선 원칙에 따라 Echo로 폴백
            // ⬇️ 왜 폴백됐는지 원인까지 남김
            log.warn("LLM call failed → Echo fallback. reason={}", ex.toString());
            answer = buildEcho(request.getMessage());
        }

        long latencyMs = (System.nanoTime() - t0) / 1_000_000;
        ChatUsage usage = new ChatUsage(estimateTokens(request.getMessage()), estimateTokens(answer));

        log.debug("chat() done: sessionId={}, latencyMs={}, promptTok={}, completionTok={}",
                sessionId, latencyMs, usage.promptTokens(), usage.completionTokens());

        return new ChatResult(answer, sessionId, usage);
    }

    /**
     * 전달받은 세션 ID를 검증하고 없으면 새 UUID를 생성한다.
     * @param sessionId 클라이언트가 전달한 세션 식별자
     * @return 공백이 아닌 최종 세션 ID
     */
    private String resolveSessionId(String sessionId) {
        return StringUtils.hasText(sessionId) ? sessionId : UUID.randomUUID().toString();
    }

    /**
     * Echo 폴백 응답을 생성한다.
     * @param message 원본 사용자 메시지
     * @return "Echo: {trimmed message}" 형태의 문자열
     */
    private String buildEcho(String message) {
        String m = (message == null) ? "" : message.trim();
        return "Echo: " + m;
    }

    /**
     * 문자열 길이를 기반으로 토큰 수를 간략히 추정한다.
     * <p>주의: 실제 토크나이저를 사용하지 않으므로 정확도는 낮지만 비용 대략치를 제공.</p>
     * @param text 토큰 수를 추정할 문자열
     * @return 코드포인트 길이 기반 추정 토큰 수
     */
    private int estimateTokens(String text) {
        if (!StringUtils.hasText(text)) return 0;
        return text.codePointCount(0, text.length());
    }
}
