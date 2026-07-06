package com.escrowflow.web.dto;

import java.util.List;

public record TransactionHistoryResponse(
        List<TransactionResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
