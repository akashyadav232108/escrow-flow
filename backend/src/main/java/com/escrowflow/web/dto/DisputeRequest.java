package com.escrowflow.web.dto;

import jakarta.validation.constraints.Size;

public record DisputeRequest(
        @Size(max = 5000) String reason
) {
}
