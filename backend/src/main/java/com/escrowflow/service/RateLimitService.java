package com.escrowflow.service;

import com.escrowflow.config.AppProperties;
import com.escrowflow.web.exception.RateLimitExceededException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;
    private final AppProperties appProperties;

    public RateLimitService(StringRedisTemplate redisTemplate, AppProperties appProperties) {
        this.redisTemplate = redisTemplate;
        this.appProperties = appProperties;
    }

    public void checkLoginRateLimit(String identifier) {
        checkRateLimit("login", identifier, appProperties.getRateLimit().getLoginPerMinute());
    }

    public void checkSignupRateLimit(String identifier) {
        checkRateLimit("signup", identifier, appProperties.getRateLimit().getSignupPerMinute());
    }

    public void checkSendOtpRateLimit(String identifier) {
        checkRateLimit("send-otp", identifier, appProperties.getRateLimit().getSendOtpPerMinute());
    }

    public void checkVerifyOtpRateLimit(String identifier) {
        // Use same limit as send-otp to prevent brute force
        checkRateLimit("verify-otp", identifier, appProperties.getRateLimit().getSendOtpPerMinute() * 2);
    }

    private void checkRateLimit(String action, String identifier, int maxRequests) {
        String key = String.format("rate-limit:%s:%s", action, identifier);
        
        Long currentCount = redisTemplate.opsForValue().increment(key);
        
        if (currentCount == null) {
            log.error("Failed to increment rate limit counter for key: {}", key);
            return; // Fail open - don't block on Redis issues
        }

        if (currentCount == 1) {
            // First request - set TTL
            redisTemplate.expire(key, Duration.ofMinutes(1));
        }

        if (currentCount > maxRequests) {
            log.warn("Rate limit exceeded: action={} identifier={} count={} max={}", 
                action, identifier, currentCount, maxRequests);
            throw new RateLimitExceededException(
                String.format("Too many %s attempts. Please try again later.", action)
            );
        }

        log.debug("Rate limit check passed: action={} identifier={} count={}/{}", 
            action, identifier, currentCount, maxRequests);
    }

    public void recordSuccessfulLogin(String identifier) {
        // Reset login attempts on successful login
        String key = String.format("rate-limit:login:%s", identifier);
        redisTemplate.delete(key);
    }
}
