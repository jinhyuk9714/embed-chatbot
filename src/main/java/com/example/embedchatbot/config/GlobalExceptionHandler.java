package com.example.embedchatbot.config;

import com.example.embedchatbot.service.LlmQuotaExceededException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(LlmQuotaExceededException.class)
    public ResponseEntity<Map<String, Object>> handleQuota(LlmQuotaExceededException ex) {
        return ResponseEntity.status(429).body(Map.of(
                "message", "llm_quota_exceeded",
                "detail", "OpenAI API 사용 한도가 초과되었습니다. 결제/크레딧을 확인하세요."
        ));
    }
}