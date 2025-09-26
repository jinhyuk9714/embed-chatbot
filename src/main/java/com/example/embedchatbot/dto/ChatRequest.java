// ============================================================================
// File: src/main/java/com/example/embedchatbot/dto/ChatRequest.java
// Why: 입력 길이/형식 검증 강화(과도한 payload 방지)
// ============================================================================
package com.example.embedchatbot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Map;

public class ChatRequest {

    @NotBlank
    @Size(max = 64)
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "botId must be alphanumeric plus . _ -")
    private String botId;

    @NotBlank
    @Size(min = 1, max = 4000)
    private String message;

    @Size(max = 128)
    private String sessionId;

    private Map<String, Object> meta;

    public String getBotId() { return botId; }
    public void setBotId(String botId) { this.botId = botId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public Map<String, Object> getMeta() { return meta; }
    public void setMeta(Map<String, Object> meta) { this.meta = meta; }
}
