package com.example.embedchatbot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Map;

/** Why: 입력 검증 (payload 폭주 방지) */
public class ChatRequest {

    @NotBlank
    @Size(max = 64)
    @Pattern(regexp = "^[A-Za-z0-9._-]+$")
    private String botId;

    @NotBlank
    @Size(max = 4000)
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