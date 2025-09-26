// File: src/main/java/com/example/embedchatbot/config/RateLimitConfig.java
package com.example.embedchatbot.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RateLimitConfig {

    private final Map<String, Bucket> ipBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> sessBuckets = new ConcurrentHashMap<>();

    private final long ipCapacity, ipRefillTokens, ipRefillPeriodSec;
    private final long sessCapacity, sessRefillTokens, sessRefillPeriodSec;

    public RateLimitConfig(
            @Value("${app.ratelimit.ip.capacity:60}") long ipCapacity,
            @Value("${app.ratelimit.ip.refillTokens:60}") long ipRefillTokens,
            @Value("${app.ratelimit.ip.refillPeriodSec:60}") long ipRefillPeriodSec,
            @Value("${app.ratelimit.session.capacity:12}") long sessCapacity,
            @Value("${app.ratelimit.session.refillTokens:12}") long sessRefillTokens,
            @Value("${app.ratelimit.session.refillPeriodSec:10}") long sessRefillPeriodSec
    ) {
        this.ipCapacity = ipCapacity;
        this.ipRefillTokens = ipRefillTokens;
        this.ipRefillPeriodSec = ipRefillPeriodSec;
        this.sessCapacity = sessCapacity;
        this.sessRefillTokens = sessRefillTokens;
        this.sessRefillPeriodSec = sessRefillPeriodSec;
    }

    public Bucket resolveIpBucket(String ip) {
        return ipBuckets.computeIfAbsent(ip == null ? "unknown" : ip, k ->
                Bucket.builder()
                        .addLimit(Bandwidth.classic(ipCapacity,
                                Refill.intervally(ipRefillTokens, Duration.ofSeconds(ipRefillPeriodSec))))
                        .build()
        );
    }

    public Bucket resolveSessionBucket(String sessionId) {
        String key = StringUtils.hasText(sessionId) ? sessionId : "no-session";
        return sessBuckets.computeIfAbsent(key, k ->
                Bucket.builder()
                        .addLimit(Bandwidth.classic(sessCapacity,
                                Refill.intervally(sessRefillTokens, Duration.ofSeconds(sessRefillPeriodSec))))
                        .build()
        );
    }
}
