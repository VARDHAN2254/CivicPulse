package com.civicpulse.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitingFilter implements Filter {

    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static class TokenBucket {
        private final int capacity;
        private final AtomicInteger tokens;
        private volatile long lastRefillTime;

        public TokenBucket(int capacity) {
            this.capacity = capacity;
            this.tokens = new AtomicInteger(capacity);
            this.lastRefillTime = Instant.now().toEpochMilli();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (tokens.get() > 0) {
                tokens.decrementAndGet();
                return true;
            }
            return false;
        }

        public int getRemainingTokens() {
            refill();
            return Math.max(0, tokens.get());
        }

        public int getCapacity() {
            return capacity;
        }

        private void refill() {
            long now = Instant.now().toEpochMilli();
            long elapsed = now - lastRefillTime;
            if (elapsed > 60_000) { // 1-minute refill cycle
                tokens.set(capacity);
                lastRefillTime = now;
            }
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String uri = httpRequest.getRequestURI();
        String clientIp = getClientIp(httpRequest);

        // Skip static assets, dev mailbox, or actuator
        if (uri.startsWith("/actuator") || uri.startsWith("/swagger-ui") || uri.startsWith("/v3/api-docs") || uri.startsWith("/api/v1/dev")) {
            chain.doFilter(request, response);
            return;
        }

        int limit = resolveLimit(uri);
        String bucketKey = clientIp + ":" + getEndpointCategory(uri);

        TokenBucket bucket = buckets.computeIfAbsent(bucketKey, k -> new TokenBucket(limit));

        httpResponse.setHeader("X-RateLimit-Limit", String.valueOf(bucket.getCapacity()));
        httpResponse.setHeader("X-RateLimit-Remaining", String.valueOf(bucket.getRemainingTokens()));

        if (!bucket.tryConsume()) {
            log.warn("Rate limit exceeded for IP [{}] on URI [{}]", clientIp, uri);
            httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            httpResponse.setHeader("Retry-After", "60");

            Map<String, Object> errorBody = Map.of(
                    "success", false,
                    "message", "Rate limit exceeded. Please slow down and try again in a few seconds.",
                    "status", 429,
                    "timestamp", Instant.now().toString()
            );

            httpResponse.getWriter().write(objectMapper.writeValueAsString(errorBody));
            return;
        }

        chain.doFilter(request, response);
    }

    private int resolveLimit(String uri) {
        if (uri.startsWith("/api/v1/auth")) {
            return 15; // 15 req/min
        }
        if (uri.contains("/registrations")) {
            return 40; // 40 req/min
        }
        if (uri.contains("/attendance")) {
            return 80; // 80 req/min
        }
        return 120; // 120 req/min default
    }

    private String getEndpointCategory(String uri) {
        if (uri.startsWith("/api/v1/auth")) return "auth";
        if (uri.contains("/registrations")) return "registrations";
        if (uri.contains("/attendance")) return "attendance";
        return "general";
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isBlank()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
