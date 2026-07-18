package com.mparsict.vosolmaliat.application.dto.response;

public record FakeSanimInformResponse(
        Long id,
        Long executionPaperId,
        String executionPaperCode,
        String messageType,
        String debtPaperId,
        String status,
        String informDate,
        String rejectionReasonCode,
        String rejectionReasonDescription,
        String restrictionRemovalDate,
        String sentAt,
        String confirmedAt,
        String errorMessage
) {}
