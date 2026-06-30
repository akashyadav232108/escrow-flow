package com.escrowflow.service;

import com.escrowflow.domain.enums.TransactionType;
import com.escrowflow.repository.WalletRepository;
import com.escrowflow.repository.WalletTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class WalletConsistencyService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    public WalletConsistencyService(
            WalletRepository walletRepository,
            WalletTransactionRepository walletTransactionRepository) {
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
    }

    @Transactional(readOnly = true)
    public boolean isWalletConsistent(Long walletId) {
        BigDecimal balance = walletRepository.findById(walletId)
                .orElseThrow()
                .getBalance();

        BigDecimal transactionSum = walletTransactionRepository.sumSignedAmountsByWalletId(
                walletId, TransactionType.CREDIT, TransactionType.DEBIT);

        return balance.compareTo(transactionSum) == 0;
    }
}
