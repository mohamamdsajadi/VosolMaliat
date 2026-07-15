package com.mparsict.vosolmaliat.application.dto.response;

public record DashboardStatsResponse(
        // Execution paper report (for pie chart)
        long notInformedExecutionPapers,
        long informedExecutionPapers,
        long activeExecutionPapers,
        long rejectedDebtPapers,
        long totalIssuedExecutionPapers,
        // Assignment report (for pie chart)
        long unassignedExecutionPapers,
        long arrestOrderIssued,
        long arrestOrderNotIssued,
        long awaitingArrestOrder,
        // My actions
        long myActionsNew,
        long myActionsAwaitingCompletion,
        long myActionsCompleted,
        long myActionsCancelled,
        // Financial collection status
        Double totalDebtAwaitingSeizure,
        Double totalDebtWithSeizureOrder,
        Double collectedAmounts,
        Double uncollectedAmounts
) {}
