package com.example.embedchatbot.dto;

public class ChatResponse {

    private final String answer;
    private final String sessionId;
    private final ChatUsage usage;
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
