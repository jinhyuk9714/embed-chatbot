// ============================================================================
// File: src/main/java/com/example/embedchatbot/config/WebConfig.java
// Why: CORS 오리진을 ENV/YAML로 제어(하드코딩 제거)
// ============================================================================
package com.example.embedchatbot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:8000,http://127.0.0.1:8000}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = StringUtils.hasText(allowedOrigins)
                ? Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(StringUtils::hasText).toList()
                : List.of("http://localhost:8000", "http://127.0.0.1:8000");

        registry.addMapping("/**")
                .allowedOrigins(origins.toArray(String[]::new))
                .allowedMethods("*")
                .allowedHeaders("*");
    }
}
