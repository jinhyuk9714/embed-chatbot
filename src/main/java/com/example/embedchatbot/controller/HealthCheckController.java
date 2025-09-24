/*
 * 헬스 체크 컨트롤러 파일.
 * - 외부 모니터링/로드밸런서가 백엔드 상태를 확인할 때 사용한다.
 * - 별도의 인증 없이 GET /health에 대한 간단한 문자열을 제공한다.
 */
package com.example.embedchatbot.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * /health 엔드포인트를 제공하는 컨트롤러.
 * <p>책임: 애플리케이션 레디니스/라이브니스 모니터링을 위한 경량 응답 제공.</p>
 */
@RestController
public class HealthCheckController {

    /**
     * 단순 텍스트 응답으로 애플리케이션이 살아있음을 알린다.
     * @return "OK" 문자열. 오케스트레이터에서 상태 체크로 활용 가능
     */
    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
