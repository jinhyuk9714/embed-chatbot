// ============================================================================
// File: src/main/java/com/example/embedchatbot/config/GlobalExceptionHandler.java
// Why: Bean Validation 실패(400) 응답 포맷 통일
// ============================================================================
package com.example.embedchatbot.config;

import com.example.embedchatbot.service.LlmQuotaExceededException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(Map.of(
                "message", "validation_error",
                "detail", detail
        ));
    }

    @ExceptionHandler(LlmQuotaExceededException.class)
    public ResponseEntity<Map<String, Object>> handleQuota(LlmQuotaExceededException ex) {
        return ResponseEntity.status(429).body(Map.of(
                "message", "llm_quota_exceeded",
                "detail", "OpenAI API 사용 한도가 초과되었습니다. 결제/크레딧을 확인하세요."
        ));
    }
}
