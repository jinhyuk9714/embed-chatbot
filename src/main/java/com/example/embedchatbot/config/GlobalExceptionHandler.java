/*
 * 전역 예외 처리기.
 * REST 컨트롤러 계층에서 발생하는 예외를 표준 응답 형식으로 변환해 API 소비자가 일관된 오류를 받을 수 있도록 한다.
 * Validation 오류는 400으로, LLM 연동 한도 초과는 429로 매핑하며, 추가 예외는 필요 시 메서드를 확장해 다룬다.
 */
package com.example.embedchatbot.config;

import com.example.embedchatbot.service.LlmQuotaExceededException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * {@link RestControllerAdvice} 기반으로 API 전체에 적용되는 예외 응답 규약을 정의한다.
 * <p>Bean Validation 실패는 Spring MVC가 400 응답을 생성하고, LLM 연동 예외는 명시적으로 429로 매핑해 호출자가 재시도 전략을 세우도록 돕는다.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * LLM 사용 한도 초과 예외를 429 상태 코드로 매핑한다.
     * @param ex OpenAI 등 외부 LLM에서 반환한 쿼터 초과 신호
     * @return HTTP 429와 간결한 오류 메시지를 담은 본문
     * <p>자세한 원인은 서버 로그에 남기고, 클라이언트에는 민감정보 없이 추상화된 메시지만 전달한다.</p>
     */
    @ExceptionHandler(LlmQuotaExceededException.class)
    public ResponseEntity<Map<String, Object>> handleQuota(LlmQuotaExceededException ex) {
        return ResponseEntity.status(429).body(Map.of(
                "message", "llm_quota_exceeded",
                "detail", "OpenAI API 사용 한도가 초과되었습니다. 결제/크레딧을 확인하세요."
        ));
    }
}
