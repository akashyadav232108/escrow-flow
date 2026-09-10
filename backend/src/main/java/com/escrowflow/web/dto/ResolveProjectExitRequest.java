package com.escrowflow.web.dto;

import com.escrowflow.domain.enums.ProjectExitOutcome;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ResolveProjectExitRequest(
        @NotNull ProjectExitOutcome projectOutcome,
        @Size(max = 5000) String adminNote,
        @Valid List<SettlementDecision> settlements
) {
    public record SettlementDecision(
            @NotNull Long milestoneId,
            @NotNull @DecimalMin(value = "0.0000", inclusive = true) BigDecimal freelancerAmount
    ) {
    }
}
