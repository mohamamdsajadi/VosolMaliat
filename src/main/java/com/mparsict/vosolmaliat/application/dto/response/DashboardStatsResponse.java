package com.mparsict.vosolmaliat.application.dto.response;

public record DashboardStatsResponse(
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
