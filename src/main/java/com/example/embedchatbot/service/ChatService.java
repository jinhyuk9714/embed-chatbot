package com.example.embedchatbot.service;

import com.example.embedchatbot.dto.ChatRequest;
import com.example.embedchatbot.dto.ChatResult;
import com.example.embedchatbot.dto.ChatUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final LlmClient llmClient;

    public ChatService(LlmClient llmClient) {
        this.llmClient = llmClient;
        // ⬇️ 앱 기동 시, 키 감지 여부를 1회 로깅(민감정보 노출 금지!)
        log.info("LLM enabled at startup: {}", llmClient != null && llmClient.enabled());
    }

    public ChatResult chat(ChatRequest request) {
        final long t0 = System.nanoTime();
        log.debug("chat() called: botId={}, hasSession={}, msgLen={}",
                request.getBotId(),
                StringUtils.hasText(request.getSessionId()),
                request.getMessage() == null ? 0 : request.getMessage().length());

        String sessionId = resolveSessionId(request.getSessionId());
        String answer;

        try {
            if (llmClient != null && llmClient.enabled()) {
                log.debug("Using LLM for response (sessionId={})", sessionId);
                String sys = "You are a helpful assistant.";
                answer = llmClient.generate(sys, request.getMessage());
            } else {
                log.debug("LLM disabled → using Echo fallback (sessionId={})", sessionId);
                answer = buildEcho(request.getMessage());
            }
        } catch (Exception ex) {
            // ⬇️ 왜 폴백됐는지 원인까지 남김
            log.warn("LLM call failed → Echo fallback. reason={}", ex.toString());
            answer = buildEcho(request.getMessage());
        }

        long latencyMs = (System.nanoTime() - t0) / 1_000_000;
        ChatUsage usage = new ChatUsage(estimateTokens(request.getMessage()), estimateTokens(answer));

        log.debug("chat() done: sessionId={}, latencyMs={}, promptTok={}, completionTok={}",
                sessionId, latencyMs, usage.promptTokens(), usage.completionTokens());

        return new ChatResult(answer, sessionId, usage);
    }

    private String resolveSessionId(String sessionId) {
        return StringUtils.hasText(sessionId) ? sessionId : UUID.randomUUID().toString();
    }

    private String buildEcho(String message) {
        String m = (message == null) ? "" : message.trim();
        return "Echo: " + m;
    }

    private int estimateTokens(String text) {
        if (!StringUtils.hasText(text)) return 0;
        return text.codePointCount(0, text.length());
    }
}
