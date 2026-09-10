package com.escrowflow.web.dto;

import jakarta.validation.constraints.Size;

public record ApplyToProjectRequest(
        @Size(max = 5000) String message
) {
}
