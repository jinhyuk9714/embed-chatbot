/*
 * 대화 API 컨트롤러 정의.
 * /v1/chat 엔드포인트를 통해 프런트/백오피스 등 외부 클라이언트가 LLM 대화 요청을 수행한다.
 * 요청 유효성 검증, 지연시간 측정, 응답 DTO 매핑 책임을 이 계층에서 집중 처리한다.
 */
package com.example.embedchatbot.controller;

import com.example.embedchatbot.dto.ChatRequest;
import com.example.embedchatbot.dto.ChatResponse;
import com.example.embedchatbot.dto.ChatResult;
import com.example.embedchatbot.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * /v1/chat 엔드포인트 컨트롤러.
 * <p>요청 유효성 검사 → 서비스 호출 → 응답 포맷 통일 및 지연시간(ms) 산출까지 담당하며,
 * Validation 오류는 GlobalExceptionHandler와 결합해 400으로 표준화된다.</p>
 */
@RestController
@RequestMapping("/v1/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 대화 요청을 처리한다.
     * @param request 필수: botId, message | 선택: sessionId, meta
     * @return answer, sessionId, usage(prompt/completion 토큰 추정치), latencyMs를 포함한 응답 DTO
     * @throws org.springframework.web.bind.MethodArgumentNotValidException 유효성 실패 시 발생하며 400으로 매핑된다.
     * <p>서비스 호출 시간 측정을 위해 ns 단위로 기록 후 ms로 환산한다.</p>
     */
    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        // 서비스 전체 지연 시간을 확인하기 위해 ns 기반 타임스탬프를 기록
        long startTime = System.nanoTime();
        // 핵심 비즈니스 로직(LLM 호출/폴백)을 수행하고 ChatResult로 전달받음
        ChatResult result = chatService.chat(request);
        // 호출 종료 시점을 다시 측정해 ms 단위 지연시간을 산출
        long latencyMs = Math.round((System.nanoTime() - startTime) / 1_000_000.0);
        // 서비스 결과를 API 응답 규격(ChatResponse)으로 변환해 반환
        ChatResponse response = new ChatResponse(result.answer(), result.sessionId(), result.usage(), latencyMs);
        return ResponseEntity.ok(response);
    }
}
