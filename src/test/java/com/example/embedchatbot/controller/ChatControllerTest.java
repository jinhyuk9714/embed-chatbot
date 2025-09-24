/*
 * ChatController 단위 테스트.
 * - MockMvc를 사용해 /v1/chat HTTP 계약을 검증한다.
 * - 성공/검증 실패 시나리오를 분리해 Given/When/Then을 명확히 한다.
 */
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

@WebMvcTest(controllers = ChatController.class)
class ChatControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean ChatService chatService; // ✅ 이거!

    @DisplayName("POST /v1/chat returns chat response")
    @Test
    void chat_success() throws Exception {
        // Given: 최소 필수 필드를 채운 ChatRequest와 서비스 모킹 결과
        ChatUsage usage = new ChatUsage(10, 12);
        ChatResult result = new ChatResult("Echo: hello", "session-123", usage);
        given(chatService.chat(any(ChatRequest.class))).willReturn(result);

        ChatRequest request = new ChatRequest();
        request.setBotId("bot-1");
        request.setMessage("hello");

        // When: /v1/chat 엔드포인트를 POST로 호출하면
        mockMvc.perform(post("/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Then: 응답 본문에 서비스 결과가 그대로 반영되고 latencyMs 필드가 존재해야 한다
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
        // Given: botId 없이 message만 포함된 요청 (유효성 실패 사례)
        ChatRequest request = new ChatRequest(); // botId 누락
        request.setMessage("hello");

        // When & Then: 필수 필드 검증 실패 시 400 Bad Request가 반환됨을 보장
        mockMvc.perform(post("/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
