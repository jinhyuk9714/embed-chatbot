/*
 * 애플리케이션 진입점.
 * 기본 HTTP 포트(9000)와 활성 프로필은 application.yml에서 정의된 값을 따른다.
 * 부트스트랩 외 다른 책임이 없으며, 실행 옵션은 SpringApplication을 통해 조정한다.
 */
package com.example.embedchatbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
/**
 * 스프링 부트 애플리케이션의 엔트리 클래스.
 * <p>SpringApplication.run(...)을 호출해 내장 서버를 기동하고, 포트/프로필은 설정 파일 및 환경변수로 제어한다.
 */
public class EmbedChatbotApplication {

    /**
     * JVM 진입점으로, 애플리케이션 부팅을 위임한다.
     * 외부 매개변수는 SpringApplication이 해석하며 별도의 전처리를 수행하지 않는다.
     */
    public static void main(String[] args) {
        SpringApplication.run(EmbedChatbotApplication.class, args);
    }
}
