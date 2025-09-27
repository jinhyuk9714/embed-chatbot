package com.example.embedchatbot.chat;

import java.util.Objects;
import java.util.UUID;

/**
 * Usage metrics that are streamed to the frontend once a response finishes.
 */
public record ChatUsage(
        int promptTokens,
        int completionTokens,
        long latencyMs,
        String traceId,
        int characters
) {
    public ChatUsage {
        traceId = traceId == null ? UUID.randomUUID().toString() : traceId;
    }

    public static ChatUsage empty() {
        return new ChatUsage(0, 0, 0L, UUID.randomUUID().toString(), 0);
    }

    public ChatUsage withLatency(long latencyMs) {
        return new ChatUsage(promptTokens, completionTokens, latencyMs, traceId, characters);
    }

    public ChatUsage withCharacters(int characters) {
        return new ChatUsage(promptTokens, completionTokens, latencyMs, traceId, characters);
    }

    public static ChatUsage mergeLatency(ChatUsage usage, long latencyMs) {
        return Objects.requireNonNullElse(usage, ChatUsage.empty()).withLatency(latencyMs);
    }
}
