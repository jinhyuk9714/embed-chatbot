/*
 * 웹 MVC 보조 설정 파일.
 * - 프런트엔드 데모(포트 8000)와 백엔드 간 CORS 허용 오리진을 지정한다.
 * - 로컬/운영 환경별 도메인 관리 시 이 파일을 참고한다.
 */
package com.example.embedchatbot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS 정책 등 웹 관련 세부 설정을 담당하는 구성 클래스.
 * <p>책임: 로컬 정적 페이지에서 백엔드 API를 호출할 수 있게 허용 오리진을 명시한다.</p>
 * <p>운영 팁: 배포 환경에서는 오리진 목록을 환경별로 분기하거나 설정화할 것을 권장한다.</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 데모용 프런트엔드가 8000 포트에서 제공되므로 동일 오리진을 화이트리스트에 추가
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:8000",
                        "http://127.0.0.1:8000")
                .allowedMethods("*")
                .allowedHeaders("*");
    }
}
