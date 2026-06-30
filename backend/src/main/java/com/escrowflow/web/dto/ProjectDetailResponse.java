package com.escrowflow.web.dto;

import com.escrowflow.domain.enums.ProjectStatus;

import java.time.Instant;
import java.util.List;

public record ProjectDetailResponse(
        Long id,
        String title,
        String description,
        ProjectStatus status,
        ProjectUserSummary client,
        ProjectUserSummary freelancer,
        List<MilestoneResponse> milestones,
        Instant createdAt
) {
}
