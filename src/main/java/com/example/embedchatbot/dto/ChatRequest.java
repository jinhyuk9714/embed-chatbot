/*
 * /v1/chat 요청 페이로드 DTO.
 * 컨트롤러 계층에서 클라이언트 입력을 검증하고, 서비스로 전달하기 위해 정의되었다.
 * 필수 필드(botId, message)에는 Bean Validation을 적용해 빠르게 오류를 감지한다.
 */
package com.example.embedchatbot.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * 클라이언트가 LLM 대화를 요청할 때 전달하는 파라미터를 보관한다.
 * <p>botId와 message는 필수이며, sessionId/meta는 선택값으로 세션 유지/추적에 활용한다.</p>
 */
public class ChatRequest {

    /** bot 인스턴스를 식별하는 필수 ID (공백 금지). */
    @NotBlank
    private String botId;

    /** 사용자 프롬프트 본문 (필수, 공백 금지). */
    @NotBlank
    private String message;

    /** 기존 세션을 이어갈 때 사용하는 선택적 세션 ID. */
    private String sessionId;

    /** 대화 메타데이터(추적용 태그, 사용자 속성 등)를 전달하는 선택 맵. */
    private Map<String, Object> meta;

    public ChatRequest() {
    }

    public String getBotId() {
        return botId;
    }

    public void setBotId(String botId) {
        this.botId = botId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public void setMeta(Map<String, Object> meta) {
        this.meta = meta;
    }
}
