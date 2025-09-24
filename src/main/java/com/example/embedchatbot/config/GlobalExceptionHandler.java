/*
 * 전역 예외 처리기 구성 파일.
 * - 컨트롤러에서 발생한 예외를 JSON 구조로 표준화한다.
 * - LLM 사용량 초과 등 공통 오류 응답을 중앙에서 관리한다.
 */
package com.example.embedchatbot.config;

import com.example.embedchatbot.service.LlmQuotaExceededException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * API 계층에서 발생한 예외를 공통 JSON 응답으로 변환하는 어드바이스.
 * <p>책임: 서비스 계층에서 던진 예외를 HTTP 상태 코드와 표준 메시지로 매핑한다.</p>
 * <p>주의: LLM 요청 전문은 로깅하지 않고, 사용자 안내에 필요한 최소 정보만 detail에 담는다.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * LLM 사용량 초과 예외를 429(Too Many Requests)로 응답한다.
     * @param ex OpenAI가 insufficient_quota를 반환했을 때 발생한 예외
     * @return 사용자에게 한도 조정/결제를 안내하는 표준 메시지
     */
    @ExceptionHandler(LlmQuotaExceededException.class)
    public ResponseEntity<Map<String, Object>> handleQuota(LlmQuotaExceededException ex) {
        // 운영자에게는 로그를 통해 추가 정보를 제공하고, 사용자에게는 간결한 안내만 노출
        return ResponseEntity.status(429).body(Map.of(
                "message", "llm_quota_exceeded",
                "detail", "OpenAI API 사용 한도가 초과되었습니다. 결제/크레딧을 확인하세요."
        ));
    }
}
