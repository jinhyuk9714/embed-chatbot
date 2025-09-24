/*
 * OpenAI WebClient 구성 파일.
 * - 외부 LLM API 호출 시 공통 타임아웃 및 메모리 제한을 정의한다.
 * - base-url/api-key는 application.yml 및 환경변수로부터 주입된다.
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
 * LLM 호출용 {@link WebClient}를 구성하는 설정 클래스.
 * <p>WebFlux 기반 WebClient를 사용해 비동기 HTTP 호출을 수행한다.</p>
 * <p>주의: 타임아웃/메모리 제한은 API 응답 크기를 고려해 조정 가능하다.</p>
 */
@Configuration
public class LlmConfig {

    /**
     * OpenAI API 호출용 WebClient 빈을 생성한다.
     * @param baseUrl OpenAI REST 엔드포인트 기본 URL
     * @param timeoutMs 응답 대기 시간(ms). 초과 시 {@code TimeoutException} 발생
     * @return LLM 호출에 사용할 {@link WebClient}
     */
    @Bean
    public WebClient openAiWebClient(
            @Value("${app.llm.base-url}") String baseUrl,
            @Value("${app.llm.timeout-ms:15000}") long timeoutMs) {

        // Reactor Netty 클라이언트에 응답 타임아웃을 적용해 장시간 대기를 방지
        HttpClient http = HttpClient.create()
                .responseTimeout(Duration.ofMillis(timeoutMs));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(http))
                .exchangeStrategies(ExchangeStrategies.builder()
                        // 스트리밍 대신 완전한 JSON 응답을 받으므로 2MB 제한으로 충분
                        .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                        .build())
                .build();
    }
}
