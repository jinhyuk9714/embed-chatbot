package com.example.embedchatbot.dto;

public record ChatResult(String answer, String sessionId, ChatUsage usage) {
}
