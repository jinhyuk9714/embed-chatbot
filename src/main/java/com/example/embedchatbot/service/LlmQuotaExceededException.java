/*
 * LLM 쿼터 초과 예외 정의 파일.
 * - OpenAI insufficient_quota 응답을 표현한다.
 * - 글로벌 예외 처리기가 429 상태 코드로 변환한다.
 */
package com.example.embedchatbot.service;

/**
 * LLM 호출 시 한도 초과 상황을 나타내는 런타임 예외.
 * <p>왜: 컨트롤러에 도달하기 전에 {@link com.example.embedchatbot.config.GlobalExceptionHandler}가 429로 매핑.</p>
 * <p>운영 팁: 로그 레벨은 WARN 이상으로 설정해 재발 시 모니터링한다.</p>
 */
public class LlmQuotaExceededException extends RuntimeException {
    public LlmQuotaExceededException(String message) { super(message); }
}