/*
 * 채팅 REST 컨트롤러 정의 파일.
 * - /v1/chat 엔드포인트에서 LLM 응답을 받아 포맷팅한다.
 * - 요청 검증, 지연시간 측정, 응답 DTO 변환을 담당한다.
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
 * <p>책임: 요청 유효성 검사 → 서비스 호출 → 응답 포맷 정규화 및 지연시간(ms) 산출.</p>
 * <p>주의: 민감한 프롬프트 전문은 로깅하지 않으며, 오류는 {@link com.example.embedchatbot.config.GlobalExceptionHandler}가 표준화한다.</p>
 */
@RestController
@RequestMapping("/v1/chat")
public class ChatController {

    private final ChatService chatService;

    /**
     * ChatService 의존성을 주입한다.
     * @param chatService LLM 호출/폴백을 담당하는 서비스
     */
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 대화 요청을 처리한다.
     * @param request 필수: botId, message | 선택: sessionId, meta
     * @return answer, sessionId, usage(prompt/completion 토큰 추정치), latencyMs(ms)
     * @throws org.springframework.web.bind.MethodArgumentNotValidException 필수 필드 누락 시 400으로 매핑
     */
    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        long startTime = System.nanoTime();
        // 서비스에서 응답 생성 후, 컨트롤러에서 응답 레이턴시를 ms 단위로 계산해 반환
        ChatResult result = chatService.chat(request);
        long latencyMs = Math.round((System.nanoTime() - startTime) / 1_000_000.0);
        // ChatService 결과를 API 응답 스키마에 맞춰 DTO로 변환
        ChatResponse response = new ChatResponse(result.answer(), result.sessionId(), result.usage(), latencyMs);
        return ResponseEntity.ok(response);
    }
}
