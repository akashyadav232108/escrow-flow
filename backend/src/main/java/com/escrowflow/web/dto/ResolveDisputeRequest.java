package com.escrowflow.web.dto;

import com.escrowflow.domain.enums.DisputeResolution;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResolveDisputeRequest(
        @NotNull DisputeResolution decision,
        @Size(max = 5000) String note
) {
}
