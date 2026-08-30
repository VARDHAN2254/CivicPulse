package com.civicpulse.ratelimit;

import com.civicpulse.common.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${civicpulse.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${civicpulse.rate-limit.default-limit-per-minute:60}")
    private long defaultLimitPerMinute;

    @Value("${civicpulse.rate-limit.auth-limit-per-minute:10}")
    private long authLimitPerMinute;

    public void checkRateLimit(String key, long maxRequests, Duration duration) {
        if (!rateLimitEnabled) {
            return;
        }

        String redisKey = "ratelimit:" + key;
        Long currentCount = stringRedisTemplate.opsForValue().increment(redisKey);

        if (currentCount != null && currentCount == 1) {
            stringRedisTemplate.expire(redisKey, duration);
        }

        if (currentCount != null && currentCount > maxRequests) {
            log.warn("Rate limit exceeded for key: {}. Current count: {}, Max: {}", redisKey, currentCount, maxRequests);
            throw new RateLimitExceededException("Too many requests. Please slow down and try again in a moment.");
        }
    }

    public void checkAuthRateLimit(String ipAddress) {
        checkRateLimit("auth:" + ipAddress, authLimitPerMinute, Duration.ofMinutes(1));
    }

    public void checkStandardRateLimit(String ipAddress, String action) {
        checkRateLimit("action:" + action + ":" + ipAddress, defaultLimitPerMinute, Duration.ofMinutes(1));
    }
}
