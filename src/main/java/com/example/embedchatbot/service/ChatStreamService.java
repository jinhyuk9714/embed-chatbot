// ============================================================================
// File: src/main/java/com/example/embedchatbot/service/ChatStreamService.java
// (LLM 경로 호출로 변경; 나머지 동작 동일)
// ============================================================================
package com.example.embedchatbot.service;

import com.example.embedchatbot.dto.ChatUsage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class ChatStreamService {

    private final ChatService chatService;
    private final ExecutorService exec = Executors.newCachedThreadPool();

    @Value("${stream.delayMs:8}") private long delayMs;
    @Value("${stream.heartbeatMs:15000}") private long heartbeatMs;

    public ChatStreamService(ChatService chatService) {
        this.chatService = chatService;
    }

    public interface StreamListener {
        void onToken(String tokenChunk);
        void onUsage(ChatUsage usage);
        void onDone();
        void onError(String message);
        void onHeartbeat();
    }

    public void stream(String message, String sessionId, StreamListener listener) {
        exec.submit(() -> {
            long start = System.nanoTime();
            try {
                chatService.stream(message, sessionId, new StreamListener() {
                    @Override public void onToken(String t) { listener.onToken(t); }
                    @Override public void onUsage(ChatUsage u) { listener.onUsage(u); }
                    @Override public void onDone() {
                        ChatUsage u = new ChatUsage(0,0,
                                Duration.ofNanos(System.nanoTime()-start).toMillis(),
                                UUID.randomUUID().toString(), 0);
                        listener.onUsage(u); listener.onDone();
                    }
                    @Override public void onError(String m) { listener.onError(m); }
                    @Override public void onHeartbeat() { listener.onHeartbeat(); }
                });
            } catch (Exception e) {
                listener.onError(e.getMessage() == null ? "stream_error" : e.getMessage());
            }
        });
    }

    public void scheduleHeartbeat(SseEmitter emitter) {
        exec.submit(() -> {
            while (true) {
                try {
                    Thread.sleep(heartbeatMs);
                    emitter.send(org.springframework.web.servlet.mvc.method.annotation.SseEmitter.event().name("keepalive").data(""));
                } catch (Exception e) {
                    emitter.complete();
                    return;
                }
            }
        });
    }

    // 하위호환 Sink 시그니처 유지(테스트용)
    public interface Sink {
        void token(String tokenChunk);
        void usage(ChatUsage usage);
        void done();
        void error(String message);
        default void keepalive() {}
    }
    public void stream(String botId, String message, String sessionId, Sink sink) {
        stream(message, sessionId, new StreamListener() {
            @Override public void onToken(String tokenChunk) { sink.token(tokenChunk); }
            @Override public void onUsage(ChatUsage usage) { sink.usage(usage); }
            @Override public void onDone() { sink.done(); }
            @Override public void onError(String message) { sink.error(message); }
            @Override public void onHeartbeat() { sink.keepalive(); }
        });
    }
}
