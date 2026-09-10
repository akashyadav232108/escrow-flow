package com.escrowflow.web.dto;

import com.escrowflow.domain.enums.ApplicationStatus;

import java.time.Instant;

public record ApplicationResponse(
        Long id,
        Long projectId,
        String projectTitle,
        Long freelancerId,
        String freelancerName,
        ApplicationStatus status,
        String message,
        Instant createdAt,
        Instant updatedAt
) {
}
