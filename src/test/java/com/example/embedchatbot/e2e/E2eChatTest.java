/*
 * E2E 시나리오 테스트.
 * - 실제 애플리케이션이 기동된 상태에서 LLM 호출까지 검증한다.
 * - mvn -P e2e verify 실행 시에만 동작하도록 @Tag("e2e")로 분리한다.
 */
package com.example.embedchatbot.e2e;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("e2e") // -P e2e에서만 실행
class E2eChatTest {

    @Test
    void chat_should_return_llm_answer_not_echo() {
        // Given: 9000포트로 기동한 서버와 OPENAI_API_KEY 환경변수 (LLM 실제 호출 보장)
        RestTemplate rt = new RestTemplate();
        String url = "http://127.0.0.1:9000/v1/chat";

        var body = Map.of("botId", "BOT_E2E", "message", "한 줄 요약: 스프링 부트는?");
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // When: REST 호출을 수행하여 응답을 받으면
        ResponseEntity<Map> res = rt.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        Object answer = res.getBody().get("answer");
        assertThat(answer).isInstanceOf(String.class);

        // Then: Echo 폴백이 아닌 실제 LLM 응답임을 확인해 엔드투엔드 성공을 보장
        assertThat(((String) answer)).doesNotStartWith("Echo:");
    }
}
