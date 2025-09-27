package com.example.embedchatbot.chat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PreDestroy;

@Service
public class ChatStreamService {

    public interface StreamListener {
        void onToken(String tokenChunk);
        void onUsage(ChatUsage usage);
        void onDone();
        void onError(String message);
    }

    private final ChatService chatService;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final long heartbeatMs;

    public ChatStreamService(ChatService chatService,
                             @Value("${stream.heartbeat-ms:15000}") long heartbeatMs) {
        this.chatService = chatService;
        this.heartbeatMs = heartbeatMs;
    }

    public void stream(String message, String sessionId, StreamListener listener) {
        executor.submit(() -> {
            try {
                chatService.stream(message, sessionId, listener);
            } catch (Exception ex) {
                listener.onError(ex.getMessage() == null ? "internal_error" : ex.getMessage());
            }
        });
    }

    public void startHeartbeat(SseEmitter emitter) {
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
            try {
                emitter.send(SseEmitter.event().name("keepalive").data(""));
            } catch (IOException e) {
                emitter.complete();
            }
        }, heartbeatMs, heartbeatMs, TimeUnit.MILLISECONDS);

        Runnable cancel = () -> task.cancel(true);
        emitter.onCompletion(cancel);
        emitter.onTimeout(cancel);
        emitter.onError(error -> cancel.run());
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
        scheduler.shutdownNow();
    }
}
