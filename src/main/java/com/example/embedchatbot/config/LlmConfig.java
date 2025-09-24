/*
 * LLM 호출 관련 인프라 구성 빈을 정의한다.
 * WebClient를 통해 OpenAI 호환 API와 통신하며, 타임아웃/메모리 상한 등 기본 네트워크 정책을 캡슐화한다.
 * 팀별로 다른 LLM을 쓸 경우 이 설정 클래스를 교체하거나 Bean 이름을 재정의해 확장한다.
 */
package com.example.embedchatbot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * LLM 연동에 사용할 {@link WebClient} 구성을 제공한다.
 * <p>기본 base-url과 타임아웃은 application.yml의 app.llm.* 값을 통해 환경별로 오버라이드한다.</p>
 */
@Configuration
public class LlmConfig {

    /**
     * OpenAI 호환 API 호출용 WebClient Bean을 생성한다.
     * @param baseUrl 호출 대상 기본 URL (환경변수/프로퍼티로 교체 가능)
     * @param timeoutMs 응답 대기 타임아웃(ms)
     * @return LLM API 전용 WebClient 인스턴스
     * <p>주요 헤더는 서비스 구현체(OpenAiLlmClient)에서 주입하며, 대규모 응답 대비를 위해 in-memory 버퍼 상한을 올려둔다.</p>
     */
    @Bean
    public WebClient openAiWebClient(
            @Value("${app.llm.base-url}") String baseUrl,
            @Value("${app.llm.timeout-ms:15000}") long timeoutMs) {

        HttpClient http = HttpClient.create()
                .responseTimeout(Duration.ofMillis(timeoutMs));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(http))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                        .build())
                .build();
    }
}
