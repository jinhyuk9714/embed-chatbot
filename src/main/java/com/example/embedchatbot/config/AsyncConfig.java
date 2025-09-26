// ============================================================================
// File: src/main/java/com/example/embedchatbot/config/AsyncConfig.java
// Why: 공용 ForkJoinPool 대신 전용, 작은 풀로 SSE 작업 실행(타자감 sleep 포함)
// ============================================================================
package com.example.embedchatbot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {
    @Bean(name = "sseExecutor")
    public Executor sseExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setThreadNamePrefix("sse-");
        ex.setCorePoolSize(Integer.parseInt(System.getProperty("SSE_CORE","8")));
        ex.setMaxPoolSize(Integer.parseInt(System.getProperty("SSE_MAX","16")));
        ex.setQueueCapacity(Integer.parseInt(System.getProperty("SSE_QUEUE","0"))); // 0 = 즉시 거절
        ex.setAllowCoreThreadTimeOut(true);
        ex.initialize();
        return ex;
    }
}
