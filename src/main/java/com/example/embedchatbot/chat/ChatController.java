package com.example.embedchatbot.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/v1")
@Validated
public class ChatController {

    private final ChatStreamService streamService;

    public ChatController(ChatStreamService streamService) {
        this.streamService = streamService;
    }

    @GetMapping("/chat/stream")
    public SseEmitter stream(
            @RequestParam @NotBlank @Size(max = 4000) String message,
            @RequestParam(required = false) @Size(max = 128) String sessionId
    ) {
        SseEmitter emitter = new SseEmitter(60_000L);
        streamService.startHeartbeat(emitter);
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
                    emitter.complete();
                }
            }

            @Override
            public void onError(String message) {
                try {
                    emitter.completeWithError(new RuntimeException(message));
                } catch (Exception ignored) {
                }
            }

        });
        return emitter;
    }
}
