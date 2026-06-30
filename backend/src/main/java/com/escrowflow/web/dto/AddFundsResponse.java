package com.escrowflow.web.dto;

import java.time.Instant;

public record AddFundsResponse(
        WalletResponse wallet,
        Long transactionId,
        Instant createdAt
) {
}
