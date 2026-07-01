package com.escrowflow.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import redis.embedded.RedisServer;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@TestConfiguration
public class TestRedisConfig {

    private RedisServer redisServer;

    @PostConstruct
    public void startRedis() {
        redisServer = new RedisServer(6379);
        redisServer.start();
    }

    @PreDestroy
    public void stopRedis() {
        if (redisServer != null) {
            redisServer.stop();
        }
    }
}
