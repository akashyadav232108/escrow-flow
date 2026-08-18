package com.escrowflow.web.dto;

import java.time.Instant;

public record ProjectAgreementResponse(
        Long id,
        Long projectId,
        String termsVersion,
        String termsText,
        Instant clientAcceptedAt,
        Instant freelancerAcceptedAt,
        boolean clientAccepted,
        boolean freelancerAccepted,
        boolean fullyAccepted,
        Instant createdAt
) {
}
