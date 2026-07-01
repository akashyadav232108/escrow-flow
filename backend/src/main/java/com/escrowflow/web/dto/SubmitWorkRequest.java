package com.escrowflow.web.dto;

import jakarta.validation.constraints.Size;

public record SubmitWorkRequest(
        @Size(max = 5000) String note
) {
}
