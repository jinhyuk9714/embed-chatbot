// ============================================================================
// PATCH SET 01 — drop-in replacements
// Place each file at the indicated path, overwriting existing ones.
// Java 17 / Spring Boot 3.x
// ============================================================================

/* ----------------------------------------------------------------------------
File: src/main/java/com/example/embedchatbot/config/WebConfig.java
---------------------------------------------------------------------------- */
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

    /**
     * Why: 운영/스테이징별 오리진 제어
     */
    @Value("${app.cors.allowed-origins:}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        List<String> origins = StringUtils.hasText(allowedOrigins) ? Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(StringUtils::hasText).toList() : List.of("http://localhost:8000", "http://127.0.0.1:8000");

        registry.addMapping("/**").allowedOrigins(origins.toArray(String[]::new)).allowedMethods("*").allowedHeaders("*");
    }
}