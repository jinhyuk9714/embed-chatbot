/*
 * 채팅 응답 DTO 정의 파일.
 * - 서비스 결과를 API 응답 포맷으로 직렬화한다.
 * - answer, sessionId, usage, latencyMs 필드를 포함한다.
 */
package com.example.embedchatbot.dto;

/**
 * /v1/chat 응답을 표현하는 DTO.
 * <p>무엇: LLM 응답 텍스트와 세션 ID, 토큰 사용량 추정치, 지연시간을 담는다.</p>
 * <p>주의: latencyMs는 컨트롤러에서 측정한 값으로 서버-클라이언트 왕복시간과 다를 수 있다.</p>
 */
public class ChatResponse {

    /** LLM 또는 폴백이 생성한 답변 텍스트. */
    private final String answer;
    /** 응답이 속한 세션 식별자. */
    private final String sessionId;
    /** 프롬프트/완료 토큰 사용량 추정치. */
    private final ChatUsage usage;
    /** 요청 처리에 소요된 서버측 지연시간(ms). */
    private final long latencyMs;

    public ChatResponse(String answer, String sessionId, ChatUsage usage, long latencyMs) {
        this.answer = answer;
        this.sessionId = sessionId;
        this.usage = usage;
        this.latencyMs = latencyMs;
    }

    public String getAnswer() {
        return answer;
    }

    public String getSessionId() {
        return sessionId;
    }

    public ChatUsage getUsage() {
        return usage;
    }

    public long getLatencyMs() {
        return latencyMs;
    }
}
