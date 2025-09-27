package com.example.embedchatbot.controller;

import com.example.embedchatbot.dto.ChatUsage;
import com.example.embedchatbot.service.ChatStreamService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/v1")
@Validated
public class ChatStreamController {

    private final ChatStreamService streamService;

    public ChatStreamController(ChatStreamService streamService) {
        this.streamService = streamService;
    }

    @GetMapping("/chat/stream")
    public SseEmitter stream(
            @RequestParam @NotBlank @Size(max = 64) String botId,
            @RequestParam @NotBlank @Size(max = 4000) String message,
            @RequestParam(required = false) @Size(max = 128) String sessionId
    ) {
        // 0L(무한) 대신 60초 기본 타임아웃; 프론트는 자동 재연결 권장
        SseEmitter emitter = new SseEmitter(60_000L); // +++

        streamService.scheduleHeartbeat(emitter);

        streamService.stream(message, sessionId, new ChatStreamService.StreamListener() {
            @Override
            public void onToken(String tokenChunk) {
                try {
                    emitter.send(SseEmitter.event().name("token").data(tokenChunk));
                } catch (Exception ignored) {
                }
            }

            @Override
            public void onUsage(ChatUsage usage) {
                try {
                    emitter.send(SseEmitter.event().name("usage").data(usage));
                } catch (Exception ignored) {
                }
            }

            @Override
            public void onDone() {
                try {
                    emitter.send(SseEmitter.event().name("done").data("ok"));
                } catch (Exception ignored) {
                } finally {
                    emitter.complete(); // +++
                }
            }

            @Override
            public void onError(String message) {
                try {
                    emitter.completeWithError(new RuntimeException(message));
                } catch (Exception ignored) {
                }
            }

            @Override
            public void onHeartbeat() {
                try {
                    emitter.send(SseEmitter.event().name("keepalive").data(""));
                } catch (Exception ignored) {
                }
            }
        });

        return emitter;
    }
}