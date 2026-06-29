package com.mparsict.vosolmaliat.application.dto.response;

public record IssuanceResponse(
        boolean success,
        String executionPaperUniqueCode,
        String rejectionReasonCode,
        String rejectionReasonDescription,
        String restrictionRemovalDate,
        String message
) {}
