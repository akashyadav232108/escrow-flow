package com.escrowflow.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@Slf4j
public class IdempotencyService {

    private static final String IDEMPOTENCY_KEY_PREFIX = "idem:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public IdempotencyService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public <T> Optional<T> getCachedResponse(String idempotencyKey, Class<T> responseType) {
        String key = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        
        try {
            String cachedJson = redisTemplate.opsForValue().get(key);
            
            if (cachedJson != null) {
                log.info("Idempotency cache hit: key={}", idempotencyKey);
                T response = objectMapper.readValue(cachedJson, responseType);
                return Optional.of(response);
            }
            
            log.debug("Idempotency cache miss: key={}", idempotencyKey);
            return Optional.empty();
            
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize cached response: key={}", idempotencyKey, e);
            return Optional.empty();
        }
    }

    public <T> void cacheResponse(String idempotencyKey, T response) {
        String key = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(key, json, CACHE_TTL);
            log.debug("Cached idempotent response: key={} ttl={}h", idempotencyKey, CACHE_TTL.toHours());
            
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize response for idempotency cache: key={}", idempotencyKey, e);
        }
    }

    public boolean isKeyProcessed(String idempotencyKey) {
        String key = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
