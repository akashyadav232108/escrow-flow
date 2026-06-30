package com.escrowflow.service;

import com.escrowflow.config.AppProperties;
import com.escrowflow.domain.User;
import com.escrowflow.domain.Wallet;
import com.escrowflow.domain.WalletTransaction;
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
import com.escrowflow.web.exception.EmailAlreadyExistsException;
import com.escrowflow.web.exception.InvalidCredentialsException;
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

    public AuthService(
            UserRepository userRepository,
            WalletRepository walletRepository,
            WalletTransactionRepository walletTransactionRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AppProperties appProperties) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.appProperties = appProperties;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = userRepository.save(User.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
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

        log.info("User signed up: userId={} email={} role={}", user.getId(), user.getEmail(), user.getRole());

        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(user);
        UserResponse userResponse = new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
        return new AuthResponse(token, userResponse);
    }
}
