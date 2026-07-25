package com.mparsict.vosolmaliat.application.dto.response;

import java.time.LocalDateTime;

public record FakeSanimInformResponse(
        Long id,
        Long executionPaperId,
        String executionPaperCode,
        LocalDateTime executionPaperCreatedAt,
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
