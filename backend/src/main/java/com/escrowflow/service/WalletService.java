package com.escrowflow.service;

import com.escrowflow.domain.Wallet;
import com.escrowflow.domain.WalletTransaction;
import com.escrowflow.domain.enums.ReferenceType;
import com.escrowflow.domain.enums.TransactionType;
import com.escrowflow.repository.WalletRepository;
import com.escrowflow.repository.WalletTransactionRepository;
import com.escrowflow.security.SecurityUtils;
import com.escrowflow.web.dto.AddFundsResponse;
import com.escrowflow.web.dto.TransactionHistoryResponse;
import com.escrowflow.web.dto.TransactionResponse;
import com.escrowflow.web.dto.WalletResponse;
import com.escrowflow.web.exception.ForbiddenException;
import com.escrowflow.web.exception.InsufficientBalanceException;
import com.escrowflow.web.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

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
        rejectAdminWalletAccess();
        Wallet wallet = findWalletByUserId(userId);
        return toResponse(wallet);
    }

    @Transactional
    public AddFundsResponse addFunds(Long userId, BigDecimal amount) {
        rejectAdminWalletAccess();
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
            throw new InsufficientBalanceException("Insufficient balance");
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

    @Transactional(readOnly = true)
    public TransactionHistoryResponse getTransactions(Long userId, int page, int size) {
        rejectAdminWalletAccess();
        Wallet wallet = findWalletByUserId(userId);
        Pageable pageable = PageRequest.of(page, size);
        Page<WalletTransaction> transactionPage = walletTransactionRepository
                .findByWalletIdOrderByCreatedAtDesc(wallet.getId(), pageable);

        List<TransactionResponse> content = transactionPage.getContent().stream()
                .map(this::toTransactionResponse)
                .toList();

        return new TransactionHistoryResponse(
                content,
                transactionPage.getNumber(),
                transactionPage.getSize(),
                transactionPage.getTotalElements(),
                transactionPage.getTotalPages()
        );
    }

    public Wallet findWalletByUserId(Long userId) {
        return walletRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user"));
    }

    private void rejectAdminWalletAccess() {
        if (SecurityUtils.getCurrentRole().isAdminRole()) {
            throw new ForbiddenException("Admins do not have wallet access");
        }
    }

    private WalletResponse toResponse(Wallet wallet) {
        return new WalletResponse(wallet.getId(), wallet.getBalance(), wallet.getUpdatedAt());
    }

    private TransactionResponse toTransactionResponse(WalletTransaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getReferenceType(),
                transaction.getReferenceId(),
                transaction.getBalanceAfter(),
                transaction.getCreatedAt()
        );
    }
}
