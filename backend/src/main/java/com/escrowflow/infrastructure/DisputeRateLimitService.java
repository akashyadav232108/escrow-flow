package com.escrowflow.infrastructure;

import com.escrowflow.web.exception.RateLimitExceededException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
public class DisputeRateLimitService {

    private static final String RATE_LIMIT_KEY_PREFIX = "ratelimit:dispute:";
    private static final Duration WINDOW_TTL = Duration.ofHours(24);
    private static final long MAX_DISPUTES_PER_WINDOW = 5;

    private final StringRedisTemplate redisTemplate;

    public DisputeRateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void checkAndIncrement(Long userId) {
        String key = RATE_LIMIT_KEY_PREFIX + userId;

        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) {
            log.warn("Dispute rate limit counter returned null: userId={}", userId);
            return;
        }

        if (count == 1L) {
            redisTemplate.expire(key, WINDOW_TTL);
        }

        if (count > MAX_DISPUTES_PER_WINDOW) {
            log.warn("Dispute rate limit exceeded: userId={} count={} limit={}",
                    userId, count, MAX_DISPUTES_PER_WINDOW);
            throw new RateLimitExceededException(
                    "Dispute rate limit exceeded: max " + MAX_DISPUTES_PER_WINDOW + " disputes per 24 hours");
        }

        log.debug("Dispute rate limit check passed: userId={} count={}/{}",
                userId, count, MAX_DISPUTES_PER_WINDOW);
    }
}
