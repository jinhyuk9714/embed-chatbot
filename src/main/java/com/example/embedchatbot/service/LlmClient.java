package com.example.embedchatbot.service;

public interface LlmClient {
    String generate(String systemPrompt, String userMessage) throws Exception;
    boolean enabled();
}
