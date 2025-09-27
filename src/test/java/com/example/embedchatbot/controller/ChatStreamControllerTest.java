// File: src/test/java/com/example/embedchatbot/controller/ChatStreamControllerTest.java
package com.example.embedchatbot.controller;

import com.example.embedchatbot.dto.ChatUsage;
import com.example.embedchatbot.service.ChatStreamService;
import com.example.embedchatbot.service.ChatStreamService.StreamListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Why: 컨트롤러가 SseEmitter를 반환하고, 서비스의 stream()을 호출하는지 검증.
 * 이벤트 바디까지 assert 하지 않고, 호출·타입·상태 위주로 안전 검증.
 */
@WebMvcTest(ChatStreamController.class)
class ChatStreamControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ChatStreamService streamService;

    @Test
    @DisplayName("GET /v1/chat/stream → 200 & text/event-stream, 서비스 stream 호출")
    void stream_ok() throws Exception {
        // heartbeat는 no-op
        doNothing().when(streamService).scheduleHeartbeat(any(SseEmitter.class));

        // stream(message, sessionId, StreamListener) 호출 시 즉시 토큰/usage/done 콜백 트리거
        ArgumentCaptor<StreamListener> cap = ArgumentCaptor.forClass(StreamListener.class);
        doAnswer(inv -> {
            StreamListener l = cap.getValue();
            l.onToken("E");
            l.onUsage(new ChatUsage(0, 1, 5L, "trace", 1));
            l.onDone();
            return null;
        }).when(streamService).stream(anyString(), anyString(), cap.capture());

        mvc.perform(get("/v1/chat/stream")
                        .queryParam("botId", "test-bot")
                        .queryParam("message", "hello")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));

        verify(streamService, times(1)).scheduleHeartbeat(any(SseEmitter.class));
        verify(streamService, times(1)).stream(anyString(), anyString(), any(StreamListener.class));
        assertThat(cap.getAllValues()).hasSize(1);
    }

    @Test
    @DisplayName("검증 실패: botId 누락 시 400")
    void stream_validation_error() throws Exception {
        mvc.perform(get("/v1/chat/stream")
                        .queryParam("message", "hello")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isBadRequest());
    }
}
