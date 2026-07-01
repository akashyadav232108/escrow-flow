package com.escrowflow.web.dto;

import com.escrowflow.domain.enums.MilestoneStatus;

import java.math.BigDecimal;

public record LockFundsResponse(
        Long milestoneId,
        MilestoneStatus status,
        Long escrowHoldId,
        BigDecimal walletBalance
) {
}
