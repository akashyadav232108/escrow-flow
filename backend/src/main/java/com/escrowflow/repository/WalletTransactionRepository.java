package com.escrowflow.repository;

import com.escrowflow.domain.WalletTransaction;
import com.escrowflow.domain.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    List<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId);

    @Query("""
            SELECT COALESCE(SUM(
                CASE WHEN t.type = :creditType THEN t.amount
                     WHEN t.type = :debitType THEN -t.amount
                     ELSE 0 END), 0)
            FROM WalletTransaction t
            WHERE t.wallet.id = :walletId
            """)
    BigDecimal sumSignedAmountsByWalletId(
            @Param("walletId") Long walletId,
            @Param("creditType") TransactionType creditType,
            @Param("debitType") TransactionType debitType);
}
