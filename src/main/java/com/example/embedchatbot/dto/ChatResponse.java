package com.example.embedchatbot.dto;

/** Optional: 최종 단건 응답 형태(스트리밍 아닌 일반) */
public class ChatResponse {
    private ChatResult result;
    public ChatResponse() {}
    public ChatResponse(ChatResult result) { this.result = result; }
    public ChatResult getResult() { return result; }
    public void setResult(ChatResult result) { this.result = result; }
}