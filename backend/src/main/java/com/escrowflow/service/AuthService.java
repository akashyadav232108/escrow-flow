package com.escrowflow.service;

import com.escrowflow.config.AppProperties;
import com.escrowflow.domain.User;
import com.escrowflow.domain.Wallet;
import com.escrowflow.domain.WalletTransaction;
import com.escrowflow.domain.enums.OtpPurpose;
import com.escrowflow.domain.enums.ReferenceType;
import com.escrowflow.domain.enums.TransactionType;
import com.escrowflow.repository.UserRepository;
import com.escrowflow.repository.WalletRepository;
import com.escrowflow.repository.WalletTransactionRepository;
import com.escrowflow.security.JwtService;
import com.escrowflow.web.dto.AuthResponse;
import com.escrowflow.web.dto.LoginRequest;
import com.escrowflow.web.dto.SignupRequest;
import com.escrowflow.web.dto.UserResponse;
import com.escrowflow.web.dto.ChangePasswordRequest;
import com.escrowflow.web.exception.AccountNotActiveException;
import com.escrowflow.web.exception.EmailAlreadyExistsException;
import com.escrowflow.web.exception.EmailNotVerifiedException;
import com.escrowflow.web.exception.InvalidCredentialsException;
import com.escrowflow.web.exception.InvalidCurrentPasswordException;
import com.escrowflow.web.exception.InvalidOtpException;
import com.escrowflow.web.exception.OtpResendCooldownException;
import com.escrowflow.web.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties appProperties;
    private final OtpService otpService;
    private final EmailService emailService;

    public AuthService(
            UserRepository userRepository,
            WalletRepository walletRepository,
            WalletTransactionRepository walletTransactionRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AppProperties appProperties,
            OtpService otpService,
            EmailService emailService) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.appProperties = appProperties;
        this.otpService = otpService;
        this.emailService = emailService;
    }

    @Transactional
    public void signup(SignupRequest request) {
        if (request.role().isAdminRole()) {
            throw new IllegalArgumentException(
                    "Cannot self-register as ADMIN or SUPER_ADMIN. Use marketplace roles only.");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = userRepository.save(User.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .emailVerified(false)
                .build());

        BigDecimal startingBalance = appProperties.getWallet().getStartingBalance();
        Wallet wallet = walletRepository.save(Wallet.builder()
                .user(user)
                .balance(startingBalance)
                .build());

        walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(wallet)
                .type(TransactionType.CREDIT)
                .amount(startingBalance)
                .referenceType(ReferenceType.ADD_FUNDS)
                .balanceAfter(startingBalance)
                .build());

        // Generate and send OTP
        String otp = otpService.generateAndSaveOtp(request.email(), OtpPurpose.EMAIL_VERIFICATION);
        emailService.sendOtpEmail(request.email(), otp, "EMAIL_VERIFICATION");

        log.info("User signed up (unverified): userId={} email={} role={}", user.getId(), user.getEmail(), user.getRole());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        if (!user.getEmailVerified()) {
            throw new EmailNotVerifiedException("Please verify your email before logging in");
        }

        if (user.getAccountStatus() == com.escrowflow.domain.enums.AccountStatus.SUSPENDED) {
            throw new AccountNotActiveException(
                    "ACCOUNT_SUSPENDED", "Your account has been suspended. Contact support.");
        }
        if (user.getAccountStatus() == com.escrowflow.domain.enums.AccountStatus.DELETED) {
            throw new AccountNotActiveException(
                    "ACCOUNT_DELETED", "This account has been removed.");
        }

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse verifyEmail(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getEmailVerified()) {
            throw new IllegalStateException("Email already verified");
        }

        boolean isValid = otpService.validateOtp(email, otp, OtpPurpose.EMAIL_VERIFICATION);
        if (!isValid) {
            throw new InvalidOtpException("Invalid or expired OTP code");
        }

        user.setEmailVerified(true);
        userRepository.save(user);

        // Send welcome email
        emailService.sendWelcomeEmail(email, user.getName());

        log.info("Email verified successfully: userId={} email={}", user.getId(), email);

        return buildAuthResponse(user);
    }

    @Transactional
    public void resendOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getEmailVerified()) {
            throw new IllegalStateException("Email already verified");
        }

        if (!otpService.canResendOtp(email, OtpPurpose.EMAIL_VERIFICATION)) {
            throw new OtpResendCooldownException("Please wait before requesting a new OTP");
        }

        String otp = otpService.generateAndSaveOtp(email, OtpPurpose.EMAIL_VERIFICATION);
        emailService.sendOtpEmail(email, otp, "EMAIL_VERIFICATION");

        log.info("OTP resent: email={}", email);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCurrentPasswordException();
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        log.info("Password changed: userId={}", userId);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(user);
        UserResponse userResponse =
                new UserResponse(
                        user.getId(),
                        user.getName(),
                        user.getEmail(),
                        user.getEmailVerified(),
                        user.getRole(),
                        user.getAccountStatus(),
                        user.getCreatedAt());
        return new AuthResponse(token, userResponse);
    }
}
