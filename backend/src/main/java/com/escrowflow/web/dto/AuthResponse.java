package com.escrowflow.web.dto;

public record AuthResponse(
        String token,
        UserResponse user
) {
}
