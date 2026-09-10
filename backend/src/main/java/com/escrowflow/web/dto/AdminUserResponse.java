package com.escrowflow.web.dto;

import com.escrowflow.domain.enums.UserRole;

import java.time.Instant;

public record AdminUserResponse(
        Long id,
        String name,
        String email,
        UserRole role,
        Instant createdAt,
        Long createdById,
        String createdByName
) {
}
