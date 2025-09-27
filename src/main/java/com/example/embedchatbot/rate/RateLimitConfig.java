package com.example.embedchatbot.rate;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Why: IP / 세션별 레이트리밋 버킷 팩토리 */
@Configuration
public class RateLimitConfig {

    @Value("${rl.ip.capacity:60}") private int ipCapacity;
    @Value("${rl.ip.refill.tokens:60}") private int ipRefillTokens;
    @Value("${rl.ip.refill.periodSec:60}") private int ipRefillPeriodSec;

    @Value("${rl.sess.capacity:12}") private int sessCapacity;
    @Value("${rl.sess.refill.tokens:12}") private int sessRefillTokens;
    @Value("${rl.sess.refill.periodSec:10}") private int sessRefillPeriodSec;

    private final Map<String, Bucket> ipBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> sessBuckets = new ConcurrentHashMap<>();

    public Bucket resolveIpBucket(String ip) {
        String key = StringUtils.hasText(ip) ? ip : "unknown";
        return ipBuckets.computeIfAbsent(key, k ->
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