package com.mparsict.vosolmaliat.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DebtPaperPushData(
        @JsonProperty("executionPaperID") Long executionPaperId,
        String executionPaperUniqueCode,
        String rejectionReasonCode,
        String rejectionReasonDescription,
        String nextRetryDate
) {
    public DebtPaperPushData(Long executionPaperId) {
        this(executionPaperId, null, null, null, null);
    }

    public static DebtPaperPushData from(IssuanceResponse result) {
        return new DebtPaperPushData(result.executionPaperId(), result.executionPaperUniqueCode(),
                result.rejectionReasonCode(), result.rejectionReasonDescription(),
                result.restrictionRemovalDate());
    }

    public static DebtPaperPushData empty() {
        return new DebtPaperPushData(0L);
    }
}
