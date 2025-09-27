package com.example.embedchatbot.chat;

import com.example.embedchatbot.llm.ChatModelClient;
import com.example.embedchatbot.rag.RetrievalService;
import com.example.embedchatbot.rag.Snippet;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ChatService {

    private static final String BASE_PROMPT = "You are a concise, polite assistant. " +
            "Prefer Korean answers when the user writes in Korean. " +
            "Use the supplied context when it is relevant and always cite sources if possible.";

    private final ChatModelClient modelClient;
    private final RetrievalService retrievalService;

    public ChatService(ChatModelClient modelClient, RetrievalService retrievalService) {
        this.modelClient = modelClient;
        this.retrievalService = retrievalService;
    }

    public void stream(String message, String sessionId, ChatStreamService.StreamListener listener) {
        long start = System.nanoTime();
        if (!modelClient.isEnabled()) {
            sendEcho(message, start, listener);
            return;
        }

        List<Snippet> context = retrievalService.retrieve(message, detectLocale(message), null);
        List<Map<String, String>> payload = buildPrompt(message, context);
        AtomicInteger charCount = new AtomicInteger();
        AtomicBoolean finished = new AtomicBoolean(false);

        modelClient.streamChat(payload, sessionId, new ChatModelClient.StreamHandler() {
            private ChatUsage usage;

            @Override
            public void onToken(String token) {
                if (!finished.get() && token != null && !token.isEmpty()) {
                    charCount.addAndGet(token.length());
                    listener.onToken(token);
                }
            }

            @Override
            public void onUsage(ChatUsage u) {
                if (!finished.get()) {
                    usage = u;
                }
            }

            @Override
            public void onComplete() {
                if (finished.compareAndSet(false, true)) {
                    ChatUsage finalUsage = ChatUsage.mergeLatency(usage,
                            Duration.ofNanos(System.nanoTime() - start).toMillis())
                            .withCharacters(charCount.get());
                    listener.onUsage(finalUsage);
                    listener.onDone();
                }
            }

            @Override
            public void onError(Throwable error) {
                if (finished.compareAndSet(false, true)) {
                    sendEcho(message, start, listener);
                }
            }
        });
    }

    private List<Map<String, String>> buildPrompt(String message, List<Snippet> context) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", BASE_PROMPT));
        if (!context.isEmpty()) {
            messages.add(Map.of(
                    "role", "system",
                    "content", "Relevant knowledge base entries:\n" + formatContext(context)
            ));
        }
        messages.add(Map.of("role", "user", "content", message));
        return messages;
    }

    private String detectLocale(String message) {
        if (!StringUtils.hasText(message)) {
            return "ko";
        }
        return message.chars().anyMatch(ch -> ch >= 0xAC00 && ch <= 0xD7A3) ? "ko" : "en";
    }

    private String formatContext(List<Snippet> context) {
        StringBuilder sb = new StringBuilder();
        for (Snippet snippet : context) {
            sb.append("- ").append(snippet.title);
            if (StringUtils.hasText(snippet.url)) {
                sb.append(" (" + snippet.url + ")");
            }
            sb.append('\n').append(snippet.text.trim()).append("\n\n");
        }
        return sb.toString();
    }

    private void sendEcho(String message, long start, ChatStreamService.StreamListener listener) {
        String normalized = message == null ? "" : message.trim();
        String text = normalized.isEmpty() ? "안녕하세요!" : "Echo: " + normalized;
        listener.onToken(text);
        ChatUsage usage = ChatUsage.mergeLatency(null,
                Duration.ofNanos(System.nanoTime() - start).toMillis())
                .withCharacters(text.length());
        listener.onUsage(usage);
        listener.onDone();
    }
}
