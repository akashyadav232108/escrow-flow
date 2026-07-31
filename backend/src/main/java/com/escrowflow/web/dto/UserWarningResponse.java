package com.escrowflow.web.dto;

import java.time.Instant;

public record UserWarningResponse(
        Long id,
        String reason,
        Long issuedByAdminId,
        String issuedByAdminName,
        Instant createdAt
) {
}
