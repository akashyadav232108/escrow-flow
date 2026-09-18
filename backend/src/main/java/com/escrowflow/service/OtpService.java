package com.escrowflow.service;

import com.escrowflow.config.AppProperties;
import com.escrowflow.domain.OtpCode;
import com.escrowflow.domain.enums.OtpPurpose;
import com.escrowflow.repository.OtpCodeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@Slf4j
public class OtpService {

    private final OtpCodeRepository otpCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(
            OtpCodeRepository otpCodeRepository,
            PasswordEncoder passwordEncoder,
            AppProperties appProperties) {
        this.otpCodeRepository = otpCodeRepository;
        this.passwordEncoder = passwordEncoder;
        this.appProperties = appProperties;
    }

    @Transactional
    public String generateAndSaveOtp(String email, OtpPurpose purpose) {
        // Delete any existing OTP for this email and purpose
        otpCodeRepository.deleteByEmailAndPurpose(email, purpose);

        // Generate new OTP
        String otp = generateOtp();
        String otpHash = passwordEncoder.encode(otp);

        Instant expiresAt = Instant.now().plus(appProperties.getOtp().getTtlMinutes(), ChronoUnit.MINUTES);

        OtpCode otpCode = OtpCode.builder()
                .email(email)
                .otpHash(otpHash)
                .purpose(purpose)
                .expiresAt(expiresAt)
                .build();

        otpCodeRepository.save(otpCode);
        log.info("OTP generated for email={} purpose={}", email, purpose);

        return otp;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean validateOtp(String email, String otp, OtpPurpose purpose) {
        Optional<OtpCode> otpCodeOpt = otpCodeRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(email, purpose);

        if (otpCodeOpt.isEmpty()) {
            log.warn("No OTP found for email={} purpose={}", email, purpose);
            return false;
        }

        OtpCode otpCode = otpCodeOpt.get();

        if (otpCode.isExpired()) {
            log.warn("OTP expired for email={} purpose={}", email, purpose);
            otpCodeRepository.delete(otpCode);
            return false;
        }

        if (otpCode.getAttempts() >= appProperties.getOtp().getMaxAttempts()) {
            log.warn("OTP max attempts exceeded for email={} purpose={}", email, purpose);
            otpCodeRepository.delete(otpCode);
            return false;
        }

        otpCode.incrementAttempts();
        otpCodeRepository.save(otpCode);

        if (!passwordEncoder.matches(otp, otpCode.getOtpHash())) {
            log.warn("Invalid OTP for email={} purpose={} attempts={}", email, purpose, otpCode.getAttempts());
            return false;
        }

        // Valid OTP - delete it
        otpCodeRepository.delete(otpCode);
        log.info("OTP validated successfully for email={} purpose={}", email, purpose);
        return true;
    }

    public boolean canResendOtp(String email, OtpPurpose purpose) {
        Optional<OtpCode> otpCodeOpt = otpCodeRepository.findTopByEmailAndPurposeOrderByCreatedAtDesc(email, purpose);
        
        if (otpCodeOpt.isEmpty()) {
            return true;
        }

        OtpCode otpCode = otpCodeOpt.get();
        Instant cooldownEndsAt = otpCode.getCreatedAt().plus(
            appProperties.getOtp().getResendCooldownSeconds(), 
            ChronoUnit.SECONDS
        );

        return Instant.now().isAfter(cooldownEndsAt);
    }

    private String generateOtp() {
        int length = appProperties.getOtp().getLength();
        int bound = (int) Math.pow(10, length);
        int otpNumber = secureRandom.nextInt(bound);
        return String.format("%0" + length + "d", otpNumber);
    }

    // Cleanup expired OTPs daily at 3 AM
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredOtps() {
        otpCodeRepository.deleteExpired(Instant.now());
        log.info("Cleaned up expired OTPs");
    }
}
