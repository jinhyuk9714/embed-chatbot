// ============================================================================
// File: src/main/java/com/example/embedchatbot/service/ChatService.java
// (LLM 사용 + 키 없을 때 Echo 폴백)
// ============================================================================
package com.example.embedchatbot.service;

import com.example.embedchatbot.dto.ChatResult;
import com.example.embedchatbot.dto.ChatUsage;
import com.example.embedchatbot.llm.LlmClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ChatService {

    private final ChatOrchestrator orchestrator;
    private final OpenAiLlmClient openAi;

    public ChatService(ChatOrchestrator orchestrator, OpenAiLlmClient openAi) {
        this.orchestrator = orchestrator;
        this.openAi = openAi;
    }

    /** Echo (폴백) */
    public ChatResult echo(String message, String sessionId) {
        String sid = StringUtils.hasText(sessionId) ? sessionId : UUID.randomUUID().toString();
        String text = "Echo: " + (message == null ? "" : message.trim());
        int chars = text.codePointCount(0, text.length());
        ChatUsage usage = new ChatUsage(0, chars, 0L, UUID.randomUUID().toString(), chars);
        return new ChatResult(sid, text, usage);
    }

    /** Stream to listener using LLM if available; else echo chunking */
    public void stream(String message, String sessionId, com.example.embedchatbot.service.ChatStreamService.StreamListener listener) {
        if (openAi.isEnabled()) {
            String system = orchestrator.systemPrompt();
            List<Map<String,String>> messages = List.of(
                    Map.of("role","system","content", system),
                    Map.of("role","user","content", message)
            );
            AtomicInteger chars = new AtomicInteger();
            openAi.streamChat(messages, sessionId, new LlmClient.Callback() {
                @Override public void onToken(String text) { chars.addAndGet(text.length()); listener.onToken(text); }
                @Override public void onUsage(ChatUsage usage) { listener.onUsage(usage); }
                @Override public void onDone() { listener.onDone(); }
                @Override public void onError(String m) { listener.onError(m); }
            });
        } else {
            // echo 토막 전송
            ChatResult res = echo(message, sessionId);
            String text = res.getText();
            for (int i = 0; i < text.length(); i++) {
                listener.onToken(String.valueOf(text.charAt(i)));
            }
            listener.onUsage(res.getUsage());
            listener.onDone();
        }
    }
}
