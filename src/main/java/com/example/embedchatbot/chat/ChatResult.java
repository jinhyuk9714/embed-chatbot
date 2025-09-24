package com.example.embedchatbot.chat;

public record ChatResult(String answer, String sessionId, ChatUsage usage) {
}
