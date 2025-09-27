package com.example.embedchatbot.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;            // +++
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/** Why: 키 검증을 레이트리밋보다 먼저 실행 */
@Component
@Order(1) // +++ ensure ApiKey runs before rate limit
public class ApiKeyFilter extends OncePerRequestFilter {
    private final String required = System.getenv().getOrDefault("CHAT_API_KEY", "");
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || uri.startsWith("/actuator") || uri.startsWith("/health") || uri.equals("/") || required.isBlank();
    }
    @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String key = req.getHeader("X-API-Key");
        if (required.isBlank() || (key != null && key.equals(required))) {
            chain.doFilter(req, res);
        } else {
            res.setStatus(HttpStatus.UNAUTHORIZED.value());
            res.setContentType("application/json");
            res.getWriter().write("{\"error\":\"unauthorized\"}");
        }
    }
}