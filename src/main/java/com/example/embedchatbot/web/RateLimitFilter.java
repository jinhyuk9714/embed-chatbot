// File: src/main/java/com/example/embedchatbot/web/RateLimitFilter.java
package com.example.embedchatbot.web;

import com.example.embedchatbot.config.RateLimitConfig;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitConfig cfg;

    public RateLimitFilter(RateLimitConfig cfg) {
        this.cfg = cfg;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !uri.startsWith("/v1/chat/stream"); // 스트림 API만 제한(필요시 확장)
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String ip = clientIp(req);
        String sid = req.getParameter("sessionId");

        Bucket ipBucket = cfg.resolveIpBucket(ip);
        Bucket sessBucket = cfg.resolveSessionBucket(sid);

        boolean ipAllowed = ipBucket.tryConsume(1);
        boolean sessAllowed = sessBucket.tryConsume(1);

        if (ipAllowed && sessAllowed) {
            setHeaders(res, ipBucket, sessBucket);
            chain.doFilter(req, res);
        } else {
            res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            res.setCharacterEncoding(StandardCharsets.UTF_8.name());
            setHeaders(res, ipBucket, sessBucket);
            res.getWriter().write("{\"error\":\"rate_limited\",\"message\":\"Too many requests\"}");
        }
    }

    private static void setHeaders(HttpServletResponse res, Bucket ipBucket, Bucket sessBucket) {
        res.setHeader("X-RateLimit-Remaining-IP", String.valueOf(ipBucket.getAvailableTokens()));
        res.setHeader("X-RateLimit-Remaining-Session", String.valueOf(sessBucket.getAvailableTokens()));
        res.setHeader("Retry-After", "10");
    }

    private static String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String xrip = req.getHeader("X-Real-IP");
        if (xrip != null && !xrip.isBlank()) return xrip.trim();
        return req.getRemoteAddr();
    }
}
