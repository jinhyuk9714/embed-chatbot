package com.example.embedchatbot.service;

import com.example.embedchatbot.dto.ChatRequest;
import com.example.embedchatbot.dto.ChatResult;
import com.example.embedchatbot.dto.ChatUsage;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
public class ChatService {

    public ChatResult chat(ChatRequest request) {
        String sessionId = resolveSessionId(request.getSessionId());
        String answer = buildAnswer(request.getMessage());
        ChatUsage usage = new ChatUsage(estimateTokens(request.getMessage()), estimateTokens(answer));
        return new ChatResult(answer, sessionId, usage);
    }

    private String resolveSessionId(String sessionId) {
        if (StringUtils.hasText(sessionId)) {
            return sessionId;
        }
        return UUID.randomUUID().toString();
    }

    private String buildAnswer(String message) {
        return "Echo: " + message;
    }

    private int estimateTokens(String text) {
        if (!StringUtils.hasText(text)) {
            return 0;
        }
        return text.length();
    }
}
