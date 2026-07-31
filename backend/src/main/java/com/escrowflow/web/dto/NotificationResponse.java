package com.escrowflow.web.dto;

import com.escrowflow.domain.enums.NotificationReferenceType;
import com.escrowflow.domain.enums.NotificationType;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String message,
        NotificationReferenceType referenceType,
        Long referenceId,
        boolean read,
        Instant createdAt
) {
}
