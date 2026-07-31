package com.escrowflow.web.dto;

import com.escrowflow.domain.enums.DisputeResolution;
import com.escrowflow.domain.enums.DisputeStatus;
import com.escrowflow.domain.enums.EscrowHoldStatus;
import com.escrowflow.domain.enums.MilestoneStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record DisputeResponse(
        Long id,
        Long milestoneId,
        String milestoneTitle,
        BigDecimal amount,
        MilestoneStatus milestoneStatus,
        EscrowHoldStatus escrowHoldStatus,
        Long projectId,
        String projectTitle,
        Long clientId,
        String clientName,
        Long freelancerId,
        String freelancerName,
        Long raisedById,
        String raisedByName,
        String reason,
        DisputeStatus status,
        DisputeResolution resolution,
        Long resolvedByAdminId,
        String resolvedByAdminName,
        String adminNote,
        String submittedNote,
        Instant createdAt,
        Instant resolvedAt
) {
}
