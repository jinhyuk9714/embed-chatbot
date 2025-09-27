package com.example.embedchatbot.service;

import org.springframework.stereotype.Component;

/** Why: 후일 RAG/툴 주입을 위한 자리. 현재는 패스스루. */
@Component
public class ChatOrchestrator {
    public String systemPrompt() {
        return "You are a concise, helpful assistant. Answer in user's language.";
    }
}