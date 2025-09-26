// File: src/test/java/com/example/embedchatbot/controller/ChatStreamControllerTest.java
package com.example.embedchatbot.controller;

import com.example.embedchatbot.service.ChatStreamService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatStreamController.class)
class ChatStreamControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    ChatStreamService streamService;

    @Test
    void stream_should_emit_tokens_and_done() throws Exception {
        // 서비스 스텁: 바로 토큰/usage/done 푸시
        Mockito.doAnswer(inv -> {
            ChatStreamService.Sink sink = inv.getArgument(3);
            sink.onTokenJson("{\"t\":\"안\"}");
            sink.onTokenJson("{\"t\":\" \"}");
            sink.onTokenJson("{\"t\":\"녕\"}");
            sink.onUsageJson("{\"promptTokens\":1,\"completionTokens\":2}");
            sink.onDone();
            return null;
        }).when(streamService).stream(eq("sample-bot"), eq("hello"), eq("sid-1"), any());

        MvcResult res = mvc.perform(get("/v1/chat/stream")
                        .param("botId", "sample-bot")
                        .param("message", "hello")
                        .param("sessionId", "sid-1")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/event-stream")))
                .andReturn();

        // 중요: UTF-8로 읽기(기본은 ISO-8859-1라 한글 깨짐)
        String body = res.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertTrue(body.contains("event:token"));
        assertTrue(body.contains("data:{\"t\":\"안\"}"));
        assertTrue(body.contains("data:{\"t\":\" \"}"));
        assertTrue(body.contains("data:{\"t\":\"녕\"}"));
        assertTrue(body.contains("event:usage"));
        assertTrue(body.contains("event:done"));
        assertTrue(body.contains("[DONE]"));
    }
}
