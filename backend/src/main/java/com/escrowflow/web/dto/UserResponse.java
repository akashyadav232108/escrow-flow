package com.escrowflow.web.dto;

import com.escrowflow.domain.enums.AccountStatus;
import com.escrowflow.domain.enums.UserRole;

import java.time.Instant;

public record UserResponse(
        Long id,
        String name,
        String email,
        Boolean emailVerified,
        UserRole role,
        AccountStatus accountStatus,
        Instant createdAt
) {
}
