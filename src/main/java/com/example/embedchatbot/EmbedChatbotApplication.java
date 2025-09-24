/*
 * 애플리케이션 엔트리포인트 파일.
 * - Spring Boot 자동 구성 기동과 실행 책임을 담당한다.
 * - 추가 설정은 config 패키지에서 분리 관리하므로 이곳에서는 main 메서드만 유지한다.
 */
package com.example.embedchatbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 애플리케이션 런처.
 * <p>책임: 스프링 컨텍스트를 초기화하고 내장 톰캣을 기동한다.</p>
 * <p>주의: 실행 옵션은 {@code application.yml} 및 환경변수에서 관리한다.</p>
 */
@SpringBootApplication
public class EmbedChatbotApplication {

    /**
     * 애플리케이션을 시작한다.
     * @param args 커맨드라인 인자(현재는 미사용)
     */
    public static void main(String[] args) {
        SpringApplication.run(EmbedChatbotApplication.class, args);
    }
}
