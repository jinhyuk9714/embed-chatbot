// ============================================================================
// File: src/main/java/com/example/embedchatbot/llm/LlmClient.java
// ============================================================================
package com.example.embedchatbot.llm;

import com.example.embedchatbot.dto.ChatUsage;

import java.util.List;
import java.util.Map;

public interface LlmClient {
    interface Callback {
        void onToken(String text);
        void onUsage(ChatUsage usage);
        void onDone();
        void onError(String message);
    }

    void streamChat(List<Map<String, String>> messages, String sessionId, Callback cb);
}
