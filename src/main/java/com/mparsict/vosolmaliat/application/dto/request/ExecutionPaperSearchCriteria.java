package com.mparsict.vosolmaliat.application.dto.request;

public record ExecutionPaperSearchCriteria(
        String uniqueCode,
        String taxpayerName,
        String taxpayerNationalId,
        String currentState,
        String notificationStatus,
        Long taxUnitId,
        Double minDebt,
        Double maxDebt
) {
    public static ExecutionPaperSearchCriteria empty() {
        return new ExecutionPaperSearchCriteria(null, null, null, null, null, null, null, null);
    }
}
