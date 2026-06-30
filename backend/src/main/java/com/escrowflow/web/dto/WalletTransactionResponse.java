package com.escrowflow.web.dto;

import com.escrowflow.domain.enums.ReferenceType;
import com.escrowflow.domain.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record WalletTransactionResponse(
        Long id,
        TransactionType type,
        BigDecimal amount,
        ReferenceType referenceType,
        Long referenceId,
        BigDecimal balanceAfter,
        Instant createdAt
) {
}
