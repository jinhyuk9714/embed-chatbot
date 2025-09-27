package com.example.embedchatbot.dto;

public class ChatResult {
    private String sessionId;
    private String text;
    private ChatUsage usage;

    public ChatResult() {}
    public ChatResult(String sessionId, String text, ChatUsage usage) {
        this.sessionId = sessionId;
        this.text = text;
        this.usage = usage;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public ChatUsage getUsage() { return usage; }
    public void setUsage(ChatUsage usage) { this.usage = usage; }
}