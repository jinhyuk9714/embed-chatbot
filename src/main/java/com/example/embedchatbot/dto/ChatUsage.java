package com.example.embedchatbot.dto;

public class ChatUsage {
    private int promptTokens;
    private int completionTokens;
    private long latencyMs;
    private String traceId;
    private int chars;

    public ChatUsage() {}
    public ChatUsage(int promptTokens, int completionTokens, long latencyMs, String traceId, int chars) {
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.latencyMs = latencyMs;
        this.traceId = traceId;
        this.chars = chars;
    }

    public int getPromptTokens() { return promptTokens; }
    public void setPromptTokens(int promptTokens) { this.promptTokens = promptTokens; }
    public int getCompletionTokens() { return completionTokens; }
    public void setCompletionTokens(int completionTokens) { this.completionTokens = completionTokens; }
    public long getLatencyMs() { return latencyMs; }
    public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public int getChars() { return chars; }
    public void setChars(int chars) { this.chars = chars; }
}