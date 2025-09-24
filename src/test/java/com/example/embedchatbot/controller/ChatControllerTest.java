package com.example.embedchatbot.controller;

import com.example.embedchatbot.dto.ChatRequest;
import com.example.embedchatbot.dto.ChatResult;
import com.example.embedchatbot.dto.ChatUsage;
import com.example.embedchatbot.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean; // ✅ 이거!
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ChatController 슬라이스 테스트.
 * <p>/v1/chat 엔드포인트가 정상 응답/유효성 검증 오류를 올바르게 처리하는지 검증한다.</p>
 */
@WebMvcTest(controllers = ChatController.class)
class ChatControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean ChatService chatService; // ✅ 이거!

    @DisplayName("POST /v1/chat returns chat response")
    @Test
    void chat_success() throws Exception {
        // Given
        ChatUsage usage = new ChatUsage(10, 12);
        ChatResult result = new ChatResult("Echo: hello", "session-123", usage);
        given(chatService.chat(any(ChatRequest.class))).willReturn(result);

        ChatRequest request = new ChatRequest();
        request.setBotId("bot-1");
        request.setMessage("hello");

        // When
        mockMvc.perform(post("/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer", is("Echo: hello")))
                .andExpect(jsonPath("$.sessionId", is("session-123")))
                .andExpect(jsonPath("$.usage.promptTokens", is(10)))
                .andExpect(jsonPath("$.usage.completionTokens", is(12)))
                .andExpect(jsonPath("$.latencyMs").exists());
    }

    @DisplayName("POST /v1/chat validates required fields")
    @Test
    void chat_missingRequiredFields() throws Exception {
        // Given: botId 누락 시 400 반환 보장
        ChatRequest request = new ChatRequest();
        request.setMessage("hello");

        // When
        mockMvc.perform(post("/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Then
                .andExpect(status().isBadRequest());
    }
}
