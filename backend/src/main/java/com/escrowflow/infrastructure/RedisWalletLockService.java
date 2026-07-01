package com.escrowflow.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

@Service
@Slf4j
public class RedisWalletLockService {

    private static final String LOCK_KEY_PREFIX = "lock:wallet:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(5);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final Duration RETRY_DELAY = Duration.ofMillis(100);

    private static final String RELEASE_SCRIPT = """
            if redis.call("get", KEYS[1]) == ARGV[1] then
                return redis.call("del", KEYS[1])
            else
                return 0
            end
            """;

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> releaseScript;

    public RedisWalletLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.releaseScript = new DefaultRedisScript<>(RELEASE_SCRIPT, Long.class);
    }

    public String acquireLock(Long walletId) {
        String lockKey = LOCK_KEY_PREFIX + walletId;
        String requestId = UUID.randomUUID().toString();

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, requestId, LOCK_TTL);

            if (Boolean.TRUE.equals(acquired)) {
                log.debug("Wallet lock acquired: walletId={} requestId={} attempt={}", 
                        walletId, requestId, attempt);
                return requestId;
            }

            if (attempt < MAX_RETRY_ATTEMPTS) {
                try {
                    Thread.sleep(RETRY_DELAY.toMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new WalletLockException("Lock acquisition interrupted for wallet: " + walletId);
                }
            }
        }

        log.warn("Failed to acquire wallet lock after {} attempts: walletId={}", 
                MAX_RETRY_ATTEMPTS, walletId);
        throw new WalletLockException("Wallet is busy, please retry");
    }

    public void releaseLock(Long walletId, String requestId) {
        String lockKey = LOCK_KEY_PREFIX + walletId;
        
        try {
            Long result = redisTemplate.execute(
                    releaseScript,
                    Collections.singletonList(lockKey),
                    requestId
            );

            if (Long.valueOf(1L).equals(result)) {
                log.debug("Wallet lock released: walletId={} requestId={}", walletId, requestId);
            } else {
                log.warn("Lock already expired or owned by another request: walletId={} requestId={}", 
                        walletId, requestId);
            }
        } catch (Exception e) {
            log.error("Error releasing wallet lock: walletId={} requestId={}", walletId, requestId, e);
        }
    }
}
