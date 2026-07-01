package com.escrowflow.web.dto;

import com.escrowflow.domain.enums.EscrowHoldStatus;
import com.escrowflow.domain.enums.MilestoneStatus;

public record MilestoneActionResponse(
        Long milestoneId,
        MilestoneStatus status,
        EscrowHoldStatus escrowHoldStatus
) {
}
