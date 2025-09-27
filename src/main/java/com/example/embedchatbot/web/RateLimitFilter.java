package com.example.embedchatbot.web;

import com.example.embedchatbot.config.RateLimitConfig;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;           // +++
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Why: /v1/chat/stream 에 대해 IP+세션 제한 */
@Component
@Order(2) // +++ after ApiKeyFilter
public class RateLimitFilter extends OncePerRequestFilter {
    private final RateLimitConfig config;
    public RateLimitFilter(RateLimitConfig config) { this.config = config; }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || (!uri.startsWith("/v1/chat/stream"));
    }
    @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String ip = clientIp(req);
        String sessionId = req.getParameter("sessionId");

        Bucket ipBucket = config.resolveIpBucket(ip);
        Bucket sessBucket = config.resolveSessionBucket(sessionId);

        if (ipBucket.tryConsume(1) && sessBucket.tryConsume(1)) {
            chain.doFilter(req, res);
            return;
        }

        // --- set headers BEFORE body ---
        res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setHeader("X-RateLimit-Remaining-IP", String.valueOf(ipBucket.getAvailableTokens()));
        res.setHeader("X-RateLimit-Remaining-Session", String.valueOf(sessBucket.getAvailableTokens()));
        res.setHeader("Retry-After", "10");
        byte[] body = "{\"error\":\"rate_limited\"}".getBytes(StandardCharsets.UTF_8);
        res.getOutputStream().write(body);
    }
    private static String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        String xrip = req.getHeader("X-Real-IP");
        if (xrip != null && !xrip.isBlank()) return xrip.trim();
        return req.getRemoteAddr();
    }
}