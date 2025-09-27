// ============================================================================
// File: src/main/java/com/example/embedchatbot/config/OpenAiClientConfig.java
// ============================================================================
package com.example.embedchatbot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
public class OpenAiClientConfig {

    @Bean
    public WebClient openAiWebClient(
            @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${openai.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${openai.read-timeout-ms:60000}") int readTimeoutMs
    ) {
        ConnectionProvider pool = ConnectionProvider.builder("openai-pool")
                .maxConnections(50)
                .pendingAcquireMaxCount(200)
                .build();

        HttpClient http = HttpClient.create(pool)
                .responseTimeout(Duration.ofMillis(readTimeoutMs));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(http))
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                        .build())
                .build();
    }
}
