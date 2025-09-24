/*
 * LLM 호출 계약을 정의하는 인터페이스.
 * 구현체는 동기 generate() 호출을 제공하며, 향후 스트리밍 API 확장 시 이 인터페이스를 교체/확장한다.
 */
package com.example.embedchatbot.service;

/**
 * LLM 호출 기능의 최소 계약을 정의한다.
 * <p>현재는 단일 턴 동기 호출만 지원하지만, 추후 스트리밍/멀티턴 지원을 위해 메서드 확장 가능성을 염두에 둔다.</p>
 */
public interface LlmClient {
    /**
     * 시스템 프롬프트와 사용자 메시지를 전달해 응답을 생성한다.
     * @param systemPrompt 어시스턴트 기본 지침(없으면 null)
     * @param userMessage 사용자 입력(필수)
     * @return LLM이 생성한 텍스트 응답
     * @throws Exception 네트워크/429/쿼터 초과 등 외부 API 호출 실패 시 발생하며 상위에서 폴백 처리한다.
     */
    String generate(String systemPrompt, String userMessage) throws Exception;

    /**
     * 클라이언트가 활성 상태인지 여부를 알려준다.
     * <p>API 키 미설정 등으로 호출 불가할 때 false를 반환하며, 호출자는 폴백 경로를 준비한다.</p>
     */
    boolean enabled();
}
