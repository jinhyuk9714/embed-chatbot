/*
 * LLM 호출 시 추정된 토큰 사용량 DTO.
 * 정확한 토크나이저 대신 문자열 길이 기반의 간이 계산 결과를 노출하므로 참고 지표로만 사용한다.
 */
package com.example.embedchatbot.dto;

/**
 * 프롬프트/응답 각각의 토큰 추정치를 보관한다.
 * <p>실제 토큰 수와 오차가 발생할 수 있으므로 과금/정확성 판단 시 주의한다.</p>
 */
public record ChatUsage(
        /** 사용자 입력(prompt)에 해당하는 추정 토큰 수. */ int promptTokens,
        /** 모델 응답(completion)에 해당하는 추정 토큰 수. */ int completionTokens) {
}
