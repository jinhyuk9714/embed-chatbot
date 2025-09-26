package com.example.embedchatbot.service;

import com.example.embedchatbot.dto.ChatRequest;
import com.example.embedchatbot.dto.ChatResult;
import com.example.embedchatbot.model.BotConfig;
import org.springframework.stereotype.Service;

import java.util.Deque;
import java.util.stream.Collectors;

@Service
public class ChatOrchestrator {

    private final ChatService chatService;    // 기존 구현 재사용
    private final BotRegistry bots;
    private final MemoryStore memory;

    public ChatOrchestrator(ChatService chatService, BotRegistry bots, MemoryStore memory) {
        this.chatService = chatService;
        this.bots = bots;
        this.memory = memory;
    }

    public ChatResult chatWithContext(String botId, String sessionId, String userMessage) {
        BotConfig cfg = bots.get(botId);

        // 1) 과거 대화 불러오기
        Deque<MemoryStore.Msg> hist = memory.get(sessionId);

        // 2) 컨텍스트 합치기(심플 포맷)
        String context = buildContext(cfg.systemPrompt(), hist, userMessage);

        // 3) 기존 ChatService 호출(메시지=컨텍스트)
        ChatRequest req = new ChatRequest(botId, context, sessionId);
        ChatResult res = chatService.chat(req);

        // 4) 메모리 업데이트
        if (cfg.systemPrompt() != null && hist.stream().noneMatch(m -> "system".equals(m.role))) {
            memory.add(sessionId, new MemoryStore.Msg("system", cfg.systemPrompt()));
        }
        memory.add(sessionId, new MemoryStore.Msg("user", userMessage));
        memory.add(sessionId, new MemoryStore.Msg("assistant", res != null ? safe(res.answer()) : ""));

        return res;
    }

    private static String buildContext(String system, Deque<MemoryStore.Msg> hist, String user) {
        StringBuilder sb = new StringBuilder();
        if (system != null && !system.isBlank()) {
            sb.append("[system]\n").append(system.trim()).append("\n\n");
        }
        if (hist != null && !hist.isEmpty()) {
            String h = hist.stream()
                    .map(m -> "[" + m.role + "] " + m.content)
                    .collect(Collectors.joining("\n"));
            sb.append(h).append("\n\n");
        }
        sb.append("[user] ").append(user);
        return sb.toString();
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
