// ============================================================================
// File: src/main/java/com/example/embedchatbot/controller/ChatStreamController.java
// Note: UTF-8 명시 + 수명주기 핸들러(변경 없음), 계약: token/usage(latencyMs, traceId)/done
// ============================================================================
package com.example.embedchatbot.controller;

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

    @GetMapping(path = "/chat/stream", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter stream(
            @RequestParam @NotBlank @Size(max = 64) String botId,
            @RequestParam @NotBlank @Size(min = 1, max = 4000) String message,
            @RequestParam(required = false) @Size(max = 128) String sessionId
    ) {
        final SseEmitter emitter = new SseEmitter(0L);
        emitter.onTimeout(() -> {
            try {
                emitter.send(SseEmitter.event().name("error").data("{\"error\":\"timeout\"}"));
            } catch (Exception ignore) {
            }
            emitter.complete();
        });
        emitter.onError(ex -> {
            try {
                emitter.send(SseEmitter.event().name("error").data("{\"error\":\"" + (ex.getMessage() == null ? "error" : ex.getMessage()) + "\"}"));
            } catch (Exception ignore) {
            }
        });

        streamService.stream(botId, message, sessionId, new ChatStreamService.Sink() {
            @Override
            public void onTokenJson(String j) {
                try {
                    emitter.send(SseEmitter.event().name("token").data(j));
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            }

            @Override
            public void onUsageJson(String j) {
                try {
                    emitter.send(SseEmitter.event().name("usage").data(j));
                } catch (Exception e) {
                    emitter.completeWithError(e);
                }
            }

            @Override
            public void onDone() {
                try {
                    emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                } catch (Exception ignore) {
                }
                emitter.complete();
            }

            @Override
            public void onError(String m) {
                try {
                    emitter.send(SseEmitter.event().name("error").data("{\"error\":\"" + m + "\"}"));
                } catch (Exception ignore) {
                }
                emitter.completeWithError(new RuntimeException(m));
            }

            @Override
            public void onHeartbeat() {
                try {
                    emitter.send(":");
                } catch (Exception ignore) {
                }
            }
        });
        return emitter;
    }
}
