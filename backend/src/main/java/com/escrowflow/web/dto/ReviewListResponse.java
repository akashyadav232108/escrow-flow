package com.escrowflow.web.dto;

import java.util.List;

public record ReviewListResponse(
        List<ReviewResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
