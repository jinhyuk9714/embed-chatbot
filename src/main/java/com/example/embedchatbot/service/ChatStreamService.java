package com.example.embedchatbot.service;

import com.example.embedchatbot.dto.ChatResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;

@Service
public class ChatStreamService {

    public interface Sink {
        void onTokenJson(String jsonToken);
        void onUsageJson(String jsonUsage);
        void onDone();
        void onError(String message);
        void onHeartbeat();
    }

    private final ChatOrchestrator orchestrator;                 // ⬅️ 변경
    private final Executor sseExecutor;
    private final long delayMs, heartbeatMs, targetTotalMs;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Semaphore permits = new Semaphore(Integer.parseInt(System.getProperty("SSE_PERMITS", "12")));

    public ChatStreamService(
            ChatOrchestrator orchestrator,
            @Qualifier("sseExecutor") Executor sseExecutor,
            @Value("${app.stream.delay-ms:8}") long delayMs,
            @Value("${app.stream.heartbeat-ms:15000}") long heartbeatMs,
            @Value("${app.stream.target-total-ms:2500}") long targetTotalMs
    ) {
        this.orchestrator = orchestrator;
        this.sseExecutor = sseExecutor;
        this.delayMs = Math.max(0, delayMs);
        this.heartbeatMs = Math.max(1000, heartbeatMs);
        this.targetTotalMs = Math.max(0, targetTotalMs);
    }

    public void stream(String botId, String message, String sessionId, Sink sink) {
        final String traceId = UUID.randomUUID().toString();
        final long t0 = System.nanoTime();

        CompletableFuture.runAsync(() -> {
            if (!permits.tryAcquire()) { sink.onError("busy"); return; }
            try {
                ChatResult result = orchestrator.chatWithContext(botId, sessionId, message);

                String reply = (result == null || result.answer() == null) ? "" : result.answer();
                reply = reply.replace("\r\n", "\n");

                int len = reply.codePointCount(0, reply.length());
                long effDelay = this.delayMs;
                if (len > 0 && targetTotalMs > 0) {
                    long perChar = targetTotalMs / Math.max(1, len);
                    effDelay = Math.min(this.delayMs, perChar);
                }

                long lastBeat = System.currentTimeMillis();
                long chars = 0L;

                for (int i = 0; i < reply.length(); ) {
                    int cp = reply.codePointAt(i);
                    String ch = new String(Character.toChars(cp));
                    sink.onTokenJson(mapper.createObjectNode().put("t", ch).toString());
                    i += Character.charCount(cp);
                    chars++;
                    if (effDelay > 0) try { Thread.sleep(effDelay); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); sink.onError("interrupted"); return; }
                    long now = System.currentTimeMillis();
                    if (now - lastBeat >= heartbeatMs) { sink.onHeartbeat(); lastBeat = now; }
                }

                long latencyMs = Math.max(0L, (System.nanoTime() - t0) / 1_000_000L);
                long pt = 0, ct = 0;
                if (result != null && result.usage() != null) { pt = result.usage().promptTokens(); ct = result.usage().completionTokens(); }

                ObjectNode u = mapper.createObjectNode()
                        .put("promptTokens", Math.max(0, pt))
                        .put("completionTokens", Math.max(0, ct))
                        .put("latencyMs", latencyMs)
                        .put("traceId", traceId)
                        .put("chars", chars);
                sink.onUsageJson(u.toString());
                sink.onDone();
            } catch (Throwable t) {
                sink.onError(t.getMessage() == null ? "error" : t.getMessage());
            } finally {
                permits.release();
            }
        }, sseExecutor);
    }
}
