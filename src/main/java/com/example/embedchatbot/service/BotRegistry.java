package com.example.embedchatbot.service;

import com.example.embedchatbot.model.BotConfig;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BotRegistry {
    private final Map<String, BotConfig> map = new ConcurrentHashMap<>();

    public BotRegistry() {
        map.put("sample-bot", new BotConfig("sample-bot", """
                당신은 간결하고 공손한 한국어 AI 비서입니다.
                규칙:
                - 답변은 짧고 분명하게.
                - 모르면 추측하지 말고 추가 정보를 요청.
                """, 0.3));
    }

    public BotConfig get(String botId) {
        return map.getOrDefault(botId, new BotConfig(botId, "You are a helpful and concise assistant.", 0.2));
    }

    public void put(BotConfig cfg) {
        map.put(cfg.botId(), cfg);
    }
}