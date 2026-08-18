package com.escrowflow.web.dto;

import jakarta.validation.constraints.AssertTrue;

public record AcceptApplicationRequest(
        @AssertTrue(message = "You must accept the project agreement to hire")
        boolean acceptedTerms
) {
}
