package com.example.embedchatbot.service;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MemoryStore {
    public static final class Msg {
        public final String role; // "user" | "assistant" | "system"
        public final String content;

        public Msg(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    private final Map<String, Deque<Msg>> store = new ConcurrentHashMap<>();
    private final int maxTurns = Integer.parseInt(System.getProperty("MEM_MAX_TURNS", "8")); // user/assistant 페어 기준

    public Deque<Msg> get(String sessionId) {
        return store.computeIfAbsent(sessionId == null ? "no-session" : sessionId, k -> new ArrayDeque<>());
    }

    public void add(String sessionId, Msg m) {
        Deque<Msg> q = get(sessionId);
        q.addLast(m);
        trim(q);
    }

    public void reset(String sessionId) {
        store.remove(sessionId);
    }

    private void trim(Deque<Msg> q) {
        // 최근 대화 페어 기준으로 대략 maxTurns 유지(단순 길이 제한)
        while (q.size() > maxTurns * 2 + 1) { // +1은 system 허용
            q.pollFirst();
        }
    }
}