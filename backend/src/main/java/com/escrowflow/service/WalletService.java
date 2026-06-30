package com.escrowflow.service;

import com.escrowflow.domain.Wallet;
import com.escrowflow.domain.WalletTransaction;
import com.escrowflow.domain.enums.ReferenceType;
import com.escrowflow.domain.enums.TransactionType;
import com.escrowflow.repository.WalletRepository;
import com.escrowflow.repository.WalletTransactionRepository;
import com.escrowflow.web.dto.AddFundsResponse;
import com.escrowflow.web.dto.WalletResponse;
import com.escrowflow.web.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    public WalletService(WalletRepository walletRepository, WalletTransactionRepository walletTransactionRepository) {
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
    }

    @Transactional(readOnly = true)
    public WalletResponse getWallet(Long userId) {
        Wallet wallet = findWalletByUserId(userId);
        return toResponse(wallet);
    }

    @Transactional
    public AddFundsResponse addFunds(Long userId, BigDecimal amount) {
        Wallet wallet = findWalletByUserId(userId);
        WalletTransaction transaction = credit(wallet, amount, ReferenceType.ADD_FUNDS, null);
        log.info("Funds added: userId={} amount={} newBalance={}", userId, amount, wallet.getBalance());
        return new AddFundsResponse(toResponse(wallet), transaction.getId(), transaction.getCreatedAt());
    }

    @Transactional
    public WalletTransaction credit(Wallet wallet, BigDecimal amount, ReferenceType referenceType, Long referenceId) {
        BigDecimal newBalance = wallet.getBalance().add(amount);
        wallet.setBalance(newBalance);
        wallet.setUpdatedAt(Instant.now());
        walletRepository.save(wallet);

        return walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(wallet)
                .type(TransactionType.CREDIT)
                .amount(amount)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .balanceAfter(newBalance)
                .build());
    }

    @Transactional
    public WalletTransaction debit(Wallet wallet, BigDecimal amount, ReferenceType referenceType, Long referenceId) {
        if (wallet.getBalance().compareTo(amount) < 0) {
            log.warn("Debit rejected — insufficient balance: walletId={} balance={} requested={}",
                    wallet.getId(), wallet.getBalance(), amount);
            throw new IllegalStateException("Insufficient balance");
        }

        BigDecimal newBalance = wallet.getBalance().subtract(amount);
        wallet.setBalance(newBalance);
        wallet.setUpdatedAt(Instant.now());
        walletRepository.save(wallet);

        return walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(wallet)
                .type(TransactionType.DEBIT)
                .amount(amount)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .balanceAfter(newBalance)
                .build());
    }

    public Wallet findWalletByUserId(Long userId) {
        return walletRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user"));
    }

    private WalletResponse toResponse(Wallet wallet) {
        return new WalletResponse(wallet.getId(), wallet.getBalance(), wallet.getUpdatedAt());
    }
}
