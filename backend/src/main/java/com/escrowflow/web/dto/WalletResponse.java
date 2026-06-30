package com.escrowflow.web.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record WalletResponse(
        Long id,
        BigDecimal balance,
        Instant updatedAt
) {
}
