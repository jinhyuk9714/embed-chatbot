/*
 * 토큰 사용량 DTO.
 * - 프롬프트/완료 토큰 추정치를 응답에 포함하기 위해 사용한다.
 * - 간이 추정치이므로 실제 청구와 차이가 날 수 있다.
 */
package com.example.embedchatbot.dto;

/**
 * LLM 호출에 대한 토큰 사용량 추정치.
 * <p>무엇: promptTokens(입력), completionTokens(출력) 두 정수 필드를 보유.</p>
 * <p>주의: ChatService에서 문자열 길이 기반 간이 추정을 하므로 실제 토큰화 결과와 다를 수 있다.</p>
 */
public record ChatUsage(int promptTokens, int completionTokens) {
}
