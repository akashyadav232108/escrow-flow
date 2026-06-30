package com.escrowflow;

import com.escrowflow.repository.UserRepository;
import com.escrowflow.repository.WalletRepository;
import com.escrowflow.repository.WalletTransactionRepository;
import com.escrowflow.service.AuthService;
import com.escrowflow.service.WalletConsistencyService;
import com.escrowflow.service.WalletService;
import com.escrowflow.web.dto.SignupRequest;
import com.escrowflow.domain.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class WalletIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletConsistencyService walletConsistencyService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Test
    @Transactional
    void addFunds_updatesBalanceAndAuditLog() {
        authService.signup(new SignupRequest(
                "Test User",
                "wallet-test@example.com",
                "password123",
                UserRole.CLIENT));

        var user = userRepository.findByEmail("wallet-test@example.com").orElseThrow();
        var wallet = walletRepository.findByUser_Id(user.getId()).orElseThrow();

        walletService.addFunds(user.getId(), new BigDecimal("2500.00"));

        var updatedWallet = walletRepository.findById(wallet.getId()).orElseThrow();
        assertThat(updatedWallet.getBalance()).isEqualByComparingTo(new BigDecimal("12500.0000"));
        assertThat(walletTransactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId())).hasSize(2);
        assertThat(walletConsistencyService.isWalletConsistent(wallet.getId())).isTrue();
    }
}
