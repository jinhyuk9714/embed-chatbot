package com.example.embedchatbot.chat;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        long startTime = System.nanoTime();
        ChatResult result = chatService.chat(request);
        long latencyMs = Math.round((System.nanoTime() - startTime) / 1_000_000.0);
        ChatResponse response = new ChatResponse(result.answer(), result.sessionId(), result.usage(), latencyMs);
        return ResponseEntity.ok(response);
    }
}
