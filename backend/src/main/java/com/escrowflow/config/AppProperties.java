package com.escrowflow.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Jwt jwt = new Jwt();
    private final Wallet wallet = new Wallet();
    private final Email email = new Email();
    private final Otp otp = new Otp();
    private final RateLimit rateLimit = new RateLimit();

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long expirationMs;
    }

    @Getter
    @Setter
    public static class Wallet {
        private BigDecimal startingBalance = new BigDecimal("10000");
    }

    @Getter
    @Setter
    public static class Email {
        private String from;
        private String fromName;
    }

    @Getter
    @Setter
    public static class Otp {
        private int length = 6;
        private int ttlMinutes = 10;
        private int resendCooldownSeconds = 60;
        private int maxAttempts = 5;
    }

    @Getter
    @Setter
    public static class RateLimit {
        private int loginPerMinute = 10;
        private int signupPerMinute = 5;
        private int sendOtpPerMinute = 3;
    }
}
