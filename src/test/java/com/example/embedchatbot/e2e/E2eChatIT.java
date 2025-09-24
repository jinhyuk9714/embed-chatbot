package com.example.embedchatbot.e2e;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("e2e") // -P e2e에서만 실행
class E2eChatIT {

    @Test
    void chat_should_return_llm_answer_not_echo() {
        // 전제: 서버가 9000 포트로 떠있고, OPENAI_API_KEY 환경변수가 설정되어 있어야 함
        RestTemplate rt = new RestTemplate();
        String url = "http://127.0.0.1:9000/v1/chat";

        var body = Map.of("botId", "BOT_E2E", "message", "한 줄 요약: 스프링 부트는?");
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> res = rt.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        Object answer = res.getBody().get("answer");
        assertThat(answer).isInstanceOf(String.class);

        // 에코 폴백이 아니어야 통과 (LLM 호출 성공)
        assertThat(((String) answer)).doesNotStartWith("Echo:");
    }
}
