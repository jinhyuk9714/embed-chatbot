/*
 * 서비스 내부에서 사용하는 대화 결과 DTO.
 * LLM 호출 및 폴백 로직에서 생산된 답변/세션/토큰 추정치를 컨트롤러 계층으로 전달한다.
 * API 응답 직전에 latency 정보만 추가되므로, 이 구조체는 순수 비즈니스 결과만 표현한다.
 */
package com.example.embedchatbot.dto;

/**
 * ChatService가 반환하는 내부용 결과 레코드.
 * <p>응답 본문, 세션 ID, 토큰 추정치를 포함하며, latency 계산은 컨트롤러에서 추가된다.</p>
 */
public record ChatResult(String answer, String sessionId, ChatUsage usage) {
}
