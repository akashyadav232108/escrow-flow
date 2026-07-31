package com.escrowflow.web.dto;

import java.util.List;

public record NotificationListResponse(
        List<NotificationResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
