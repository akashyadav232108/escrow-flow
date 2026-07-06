package com.escrowflow.service;

import com.escrowflow.domain.Wallet;
import com.escrowflow.domain.enums.TransactionType;
import com.escrowflow.repository.WalletRepository;
import com.escrowflow.repository.WalletTransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
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

    @Transactional(readOnly = true)
    public boolean areAllWalletsConsistent() {
        List<Wallet> allWallets = walletRepository.findAll();
        
        for (Wallet wallet : allWallets) {
            if (!isWalletConsistent(wallet.getId())) {
                log.error("Wallet inconsistency detected: walletId={} balance={}", 
                        wallet.getId(), wallet.getBalance());
                return false;
            }
        }
        
        log.info("All {} wallets passed consistency check", allWallets.size());
        return true;
    }

    @Transactional(readOnly = true)
    public List<Long> findInconsistentWalletIds() {
        return walletRepository.findAll().stream()
                .filter(wallet -> !isWalletConsistent(wallet.getId()))
                .map(Wallet::getId)
                .collect(Collectors.toList());
    }
}
