/*
 * /v1/chat 응답 DTO.
 * 서비스에서 생성된 결과를 직렬화하기 위한 읽기 전용 컨테이너로, latency/tokens 정보까지 포함한다.
 * 컨트롤러에서만 생성하며, 역직렬화 대상이 아니므로 불변 필드를 유지한다.
 */
package com.example.embedchatbot.dto;

/**
 * 대화 응답을 캡슐화하는 읽기 전용 DTO.
 * <p>클라이언트 표시용 답변 텍스트, 세션 ID, 토큰 추정치, 지연시간(ms)을 모두 전달한다.</p>
 */
public class ChatResponse {

    /** 생성된 답변 텍스트. */
    private final String answer;
    /** 세션 유지를 위한 식별자. */
    private final String sessionId;
    /** LLM 호출 시 추정된 토큰 소비량. */
    private final ChatUsage usage;
    /** 서버가 측정한 왕복 지연시간(ms). */
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
