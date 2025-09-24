/*
 * 내부 서비스 결과 DTO.
 * - 서비스 계층이 컨트롤러로 전달하는 중간 결과를 표현한다.
 * - 외부 응답 ChatResponse 변환 전 단계에서 사용한다.
 */
package com.example.embedchatbot.dto;

/**
 * 서비스 계층이 반환하는 채팅 처리 결과.
 * <p>무엇: 생성된 답변, 확정된 세션 ID, 토큰 사용 추정치를 보유한다.</p>
 * <p>왜: 컨트롤러가 latencyMs를 추가해 최종 응답을 만들기 위한 중간 DTO.</p>
 */
public record ChatResult(String answer, String sessionId, ChatUsage usage) {
}
