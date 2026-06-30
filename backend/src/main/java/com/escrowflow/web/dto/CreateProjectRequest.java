package com.escrowflow.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CreateProjectRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 5000) String description,
        @NotEmpty @Valid List<CreateMilestoneRequest> milestones
) {
    public record CreateMilestoneRequest(
            @NotBlank @Size(max = 255) String title,
            @Size(max = 5000) String description,
            @NotNull @DecimalMin(value = "0.0001", inclusive = true) BigDecimal amount
    ) {
    }
}
