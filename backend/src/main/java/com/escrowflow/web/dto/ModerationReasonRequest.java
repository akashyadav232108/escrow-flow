package com.escrowflow.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ModerationReasonRequest(
        @NotBlank @Size(max = 5000) String reason
) {
}
