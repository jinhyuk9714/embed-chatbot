/*
 * LLM 사용 한도 초과를 표현하는 런타임 예외.
 * 외부 API가 429/insufficient_quota를 반환할 때 발생시켜 상위 계층이 사용자 친화적인 메시지와 적절한 로깅 전략을 적용하도록 돕는다.
 */
package com.example.embedchatbot.service;

/**
 * LLM 쿼터 초과 상황을 표현하는 도메인 예외.
 * <p>GlobalExceptionHandler가 429 응답으로 매핑하며, 서버 로그에서는 경고 레벨로 기록해 운영자가 모니터링할 수 있게 한다.</p>
 */
public class LlmQuotaExceededException extends RuntimeException {
    public LlmQuotaExceededException(String message) { super(message); }
}