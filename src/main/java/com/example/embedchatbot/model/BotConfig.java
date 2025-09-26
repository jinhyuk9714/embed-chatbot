package com.example.embedchatbot.model;

public record BotConfig(String botId, String systemPrompt, double temperature) {
}