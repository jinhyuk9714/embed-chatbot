/*
 * 웹 MVC 부가기능 설정.
 * 현재는 CORS 설정만 담당하며, 프런트엔드 샘플 페이지(frontend/chat.html)에서 호출 시 로컬 개발 포트(8000) 접근을 허용한다.
 * 운영 환경에서는 허용 오리진을 환경 변수나 별도 프로퍼티로 덮어쓰는 방식을 권장한다.
 */
package com.example.embedchatbot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC 레벨의 교차 출처 제어를 담당한다.
 * <p>로컬 프런트엔드 개발(기본 8000 포트)과 분리된 운영 도메인을 명시적으로 관리할 수 있도록 구조를 단순화했다.</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 전체 경로에 대해 CORS 허용 오리진을 등록한다.
     * <p>샘플 HTML은 localhost/127.0.0.1:8000을 기본값으로 사용하므로 해당 포트를 열어둔다.
     * 운영 시에는 application.yml 또는 별도 설정 클래스로 허용 목록을 분리한다.</p>
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:8000",
                        "http://127.0.0.1:8000")
                .allowedMethods("*")
                .allowedHeaders("*");
    }
}
