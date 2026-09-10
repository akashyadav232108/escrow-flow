package com.escrowflow.web.dto;

import java.time.Instant;

public record ReviewResponse(
        Long id,
        Long projectId,
        Long reviewerId,
        String reviewerName,
        Long freelancerId,
        int rating,
        String comment,
        Instant createdAt
) {
}
