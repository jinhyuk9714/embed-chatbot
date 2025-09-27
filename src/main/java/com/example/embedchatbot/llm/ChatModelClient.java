package com.example.embedchatbot.llm;

import com.example.embedchatbot.chat.ChatUsage;

import java.util.List;
import java.util.Map;

public interface ChatModelClient {

    interface StreamHandler {
        void onToken(String token);
        void onUsage(ChatUsage usage);
        void onComplete();
        void onError(Throwable error);
    }

    void streamChat(List<Map<String, String>> messages, String sessionId, StreamHandler handler);

    boolean isEnabled();
}
