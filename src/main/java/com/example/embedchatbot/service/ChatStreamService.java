package com.example.embedchatbot.service;

import com.example.embedchatbot.dto.ChatRequest;
import com.example.embedchatbot.dto.ChatResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class ChatStreamService {

    public interface Sink {
        void onTokenJson(String jsonToken);   // {"t":"…"}
        void onUsageJson(String jsonUsage);   // {"promptTokens":..,"completionTokens":..}
        void onDone();
        void onError(String message);
        void onHeartbeat();                   // SSE keep-alive (":\n")
    }

    private final ChatService chatService;
    private final long delayMs;
    private final long heartbeatMs;

    public ChatStreamService(
            ChatService chatService,
            @Value("${app.stream.delay-ms:8}") long delayMs,
            @Value("${app.stream.heartbeat-ms:15000}") long heartbeatMs
    ) {
        this.chatService = chatService;
        this.delayMs = Math.max(0, delayMs);
        this.heartbeatMs = Math.max(1000, heartbeatMs);
    }

    public void stream(String botId, String message, String sessionId, Sink sink) {
        CompletableFuture.runAsync(() -> {
            try {
                final ChatRequest req = buildRequest(botId, message, sessionId);
                final ChatResult result = chatService.chat(req);

                String reply = extractAnswer(result);
                if (reply == null) reply = "";
                reply = reply.replace("\r\n", "\n");

                long lastBeat = System.currentTimeMillis();
                for (int i = 0; i < reply.length(); ) {
                    int cp = reply.codePointAt(i);
                    String ch = new String(Character.toChars(cp));
                    sink.onTokenJson("{\"t\":\"" + jsonEsc(ch) + "\"}");
                    i += Character.charCount(cp);

                    if (delayMs > 0) Thread.sleep(delayMs);
                    long now = System.currentTimeMillis();
                    if (now - lastBeat >= heartbeatMs) {
                        sink.onHeartbeat();  // ":\n"
                        lastBeat = now;
                    }
                }

                Object usage = extractUsage(result);
                if (usage != null) {
                    long pt = safeTokens(usage, true);
                    long ct = safeTokens(usage, false);
                    sink.onUsageJson("{\"promptTokens\":" + Math.max(0, pt) + ",\"completionTokens\":" + Math.max(0, ct) + "}");
                }
                sink.onDone();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                sink.onError("interrupted");
            } catch (Throwable t) {
                sink.onError(t.getMessage() == null ? "error" : t.getMessage());
            }
        });
    }

    // ---- helpers (record/POJO 호환; 리플렉션) ----
    private static ChatRequest buildRequest(String botId, String message, String sessionId) {
        try {
            return ChatRequest.class
                    .getDeclaredConstructor(String.class, String.class, String.class)
                    .newInstance(botId, message, sessionId);
        } catch (NoSuchMethodException e) {
            try {
                ChatRequest req = ChatRequest.class.getDeclaredConstructor().newInstance();
                ChatRequest.class.getMethod("setBotId", String.class).invoke(req, botId);
                ChatRequest.class.getMethod("setMessage", String.class).invoke(req, message);
                ChatRequest.class.getMethod("setSessionId", String.class).invoke(req, sessionId);
                return req;
            } catch (Exception ex) {
                throw new IllegalStateException("Cannot construct ChatRequest", ex);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Cannot construct ChatRequest", e);
        }
    }

    private static String extractAnswer(ChatResult r) {
        if (r == null) return "";
        try { return (String) r.getClass().getMethod("answer").invoke(r); }
        catch (NoSuchMethodException ignore) {
            try { return (String) r.getClass().getMethod("getAnswer").invoke(r); }
            catch (Exception e) { return ""; }
        } catch (Exception e) { return ""; }
    }
    private static Object extractUsage(ChatResult r) {
        if (r == null) return null;
        try { return r.getClass().getMethod("usage").invoke(r); }
        catch (NoSuchMethodException ignore) {
            try { return r.getClass().getMethod("getUsage").invoke(r); }
            catch (Exception e) { return null; }
        } catch (Exception e) { return null; }
    }
    private static long safeTokens(Object usage, boolean prompt) {
        try {
            if (prompt) {
                try { return ((Number) usage.getClass().getMethod("promptTokens").invoke(usage)).longValue(); }
                catch (NoSuchMethodException ignore) { return ((Number) usage.getClass().getMethod("getPromptTokens").invoke(usage)).longValue(); }
            } else {
                try { return ((Number) usage.getClass().getMethod("completionTokens").invoke(usage)).longValue(); }
                catch (NoSuchMethodException ignore) { return ((Number) usage.getClass().getMethod("getCompletionTokens").invoke(usage)).longValue(); }
            }
        } catch (Exception e) { return 0L; }
    }

    private static String jsonEsc(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> b.append("\\\\");
                case '\"' -> b.append("\\\"");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> b.append(c);
            }
        }
        return b.toString();
    }
}
