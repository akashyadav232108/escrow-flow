package com.escrowflow.web.dto;

import com.escrowflow.domain.enums.MilestoneStatus;

import java.math.BigDecimal;

public record MilestoneResponse(
        Long id,
        String title,
        String description,
        BigDecimal amount,
        MilestoneStatus status
) {
}
