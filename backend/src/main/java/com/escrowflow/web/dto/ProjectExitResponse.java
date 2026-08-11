package com.escrowflow.web.dto;

import com.escrowflow.domain.enums.MilestoneStatus;
import com.escrowflow.domain.enums.ProjectExitOutcome;
import com.escrowflow.domain.enums.ProjectExitStatus;
import com.escrowflow.domain.enums.ProjectStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProjectExitResponse(
        Long id,
        Long projectId,
        String projectTitle,
        ProjectStatus projectStatus,
        Long clientId,
        String clientName,
        Long freelancerId,
        String freelancerName,
        Long raisedById,
        String raisedByName,
        String reason,
        ProjectExitStatus status,
        ProjectExitOutcome projectOutcome,
        String adminNote,
        Long resolvedByAdminId,
        String resolvedByAdminName,
        Instant createdAt,
        Instant resolvedAt,
        List<SettlementResponse> settlements
) {
    public record SettlementResponse(
            Long id,
            Long milestoneId,
            String milestoneTitle,
            MilestoneStatus milestoneStatus,
            BigDecimal holdAmount,
            BigDecimal freelancerAmount,
            BigDecimal clientRefundAmount
    ) {
    }
}
