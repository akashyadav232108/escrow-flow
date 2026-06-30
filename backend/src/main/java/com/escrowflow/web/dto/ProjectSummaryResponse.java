package com.escrowflow.web.dto;

import com.escrowflow.domain.enums.ProjectStatus;

import java.time.Instant;

public record ProjectSummaryResponse(
        Long id,
        String title,
        ProjectStatus status,
        ProjectUserSummary client,
        ProjectUserSummary freelancer,
        Instant createdAt
) {
}
