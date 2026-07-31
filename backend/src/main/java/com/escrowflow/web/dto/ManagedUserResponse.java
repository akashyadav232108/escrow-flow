package com.escrowflow.web.dto;

import com.escrowflow.domain.enums.AccountStatus;
import com.escrowflow.domain.enums.UserRole;

import java.time.Instant;
import java.util.List;

public record ManagedUserResponse(
        Long id,
        String name,
        String email,
        UserRole role,
        AccountStatus accountStatus,
        Instant createdAt,
        Instant deletedAt,
        long warningCount,
        List<UserWarningResponse> warnings
) {
}
