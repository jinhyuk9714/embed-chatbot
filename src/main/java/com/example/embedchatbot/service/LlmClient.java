/*
 * LLM 클라이언트 계약 정의 파일.
 * - 다양한 LLM 공급자 구현(OpenAI 등)이 이 인터페이스를 구현한다.
 * - 동기 생성 API 기준이지만 추후 스트리밍 확장을 고려한다.
 */
package com.example.embedchatbot.service;

/**
 * LLM 호출을 추상화한 인터페이스.
 * <p>무엇: 동기식 응답 생성 메서드와 활성화 여부 확인 메서드를 제공한다.</p>
 * <p>왜: ChatService가 공급자 종류와 무관하게 동일한 방식으로 호출하기 위함.</p>
 */
public interface LlmClient {
    /**
     * 시스템/사용자 메시지를 기반으로 LLM 응답을 생성한다.
     * @param systemPrompt 시스템 지침(없으면 빈 문자열 전달 가능)
     * @param userMessage 사용자가 입력한 본문
     * @return 생성된 텍스트 응답
     * @throws Exception 네트워크 오류, 429 등 복구 불가능한 상황 시 예외 전파
     */
    String generate(String systemPrompt, String userMessage) throws Exception;

    /**
     * 현재 LLM 클라이언트가 활성화되었는지 여부를 반환한다.
     * <p>운영 팁: API 키 미설정 시 false가 되어 ChatService에서 폴백을 적용한다.</p>
     * @return true면 generate 호출 가능, false면 폴백 사용
     */
    boolean enabled();
}
