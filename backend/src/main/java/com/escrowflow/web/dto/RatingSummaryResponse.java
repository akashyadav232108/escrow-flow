package com.escrowflow.web.dto;

public record RatingSummaryResponse(
        double averageRating,
        long reviewCount
) {
}
