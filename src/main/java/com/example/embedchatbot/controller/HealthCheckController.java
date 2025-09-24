/*
 * 헬스체크 전용 컨트롤러.
 * ALB, Kubernetes, CI 파이프라인 등 외부 헬스체크가 서비스 생존 여부를 빠르게 판단할 수 있도록 경량 엔드포인트를 제공한다.
 * 비즈니스 로직과 분리된 단순 문자열 응답을 유지해 장애 시에도 오버헤드를 최소화한다.
 */
package com.example.embedchatbot.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * /health 엔드포인트를 노출해 가용성 모니터링을 돕는다.
 * <p>ALB, GitHub Actions E2E 테스트 등 외부 시스템이 애플리케이션의 readiness를 확인할 때 사용한다.</p>
 */
@RestController
public class HealthCheckController {

    /**
     * 간단한 문자열 "OK"를 반환해 서비스 정상 동작 여부를 알린다.
     * <p>복잡한 계산이나 외부 의존성이 없어야 하므로 DB/LLM 호출은 포함하지 않는다.</p>
     */
    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
