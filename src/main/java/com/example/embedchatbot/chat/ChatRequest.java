package com.example.embedchatbot.chat;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public class ChatRequest {

    @NotBlank
    private String botId;

    @NotBlank
    private String message;

    private String sessionId;

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
