package com.escrowflow.web.dto;

import com.escrowflow.domain.enums.UserRole;

public record UserResponse(
        Long id,
        String name,
        String email,
        UserRole role
) {
}
