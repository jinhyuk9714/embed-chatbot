package com.example.embedchatbot.controller;

import com.example.embedchatbot.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    ChatService chatService;

    @Test
    void chat_ok() throws Exception {
        when(chatService.chat(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new com.example.embedchatbot.dto.ChatResult("sid-1", "pong", null));
        mvc.perform(post("/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                    {"botId":"sample-bot","message":"hi","sessionId":"s1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("pong"));
    }

    @Test
    void chat_validation_empty_message_400() throws Exception {
        mvc.perform(post("/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                    {"botId":"sample-bot","message":"","sessionId":"s1"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("validation_error"));
    }
}