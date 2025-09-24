/*
 * 채팅 요청 DTO 정의 파일.
 * - /v1/chat POST 요청에서 역직렬화되는 필드를 보유한다.
 * - 유효성 검사를 통해 필수 필드를 보장한다.
 */
package com.example.embedchatbot.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * 채팅 요청 본문을 표현하는 DTO.
 * <p>무엇: 클라이언트가 보낸 botId, message, sessionId, meta 정보를 담는다.</p>
 * <p>왜: 컨트롤러에서 유효성 검사를 수행하고 서비스에 전달하기 위함.</p>
 */
public class ChatRequest {

    /**
     * 챗봇 구성 식별자. 여러 봇 구성(시스템 프롬프트 등)을 구분한다.
     * <p>@NotBlank로 빈 문자열을 방지해 라우팅 오류를 줄인다.</p>
     */
    @NotBlank
    private String botId;

    /**
     * 사용자가 입력한 자연어 메시지. 프롬프트 길이에 따라 토큰 비용이 결정된다.
     * <p>@NotBlank: 빈 요청을 허용하지 않음.</p>
     */
    @NotBlank
    private String message;

    /**
     * 대화 세션 식별자. 없으면 서버에서 새 UUID를 생성한다.
     * <p>동일 세션 ID를 재사용하면 캐시/대화 맥락 유지에 활용할 수 있다.</p>
     */
    private String sessionId;

    /**
     * 선택 메타데이터(클라이언트 버전, 언어 등)를 담는 자유형 Map.
     * <p>주의: 민감정보는 전달하지 말고, 키는 lowerCamelCase 사용을 권장.</p>
     */
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
