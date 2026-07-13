package com.mparsict.vosolmaliat.application.dto.response.admin;

import java.util.List;

public record AdminDataSummaryResponse(
        List<FinalizedDebtPaperSummary> finalizedDebtPapers,
        List<TaxpayerSummary> taxpayers,
        List<ExecutionPaperSummary> executionPapers
) {
    public record FinalizedDebtPaperSummary(
            Long id,
            String debtPaperUniqueCode,
            String taxSource
    ) {}

    public record TaxpayerSummary(
            Long id,
            String debtPaperUniqueCode,
            String debtPaperIssueDate,
            String type
    ) {}

    public record ExecutionPaperSummary(
            Long id,
            String executionPaperUniqueCode,
            String currentState
    ) {}
}
