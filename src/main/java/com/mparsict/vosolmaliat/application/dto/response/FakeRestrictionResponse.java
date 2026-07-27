package com.mparsict.vosolmaliat.application.dto.response;

public record FakeRestrictionResponse(
        Long id,
        String officialGazetteRequestId,
        Long executionPaperId,
        String executionPaperCode,
        String status,
        Integer relatedPersonCount,
        String payload,
        String sentAt,
        String confirmedAt,
        String errorMessage
) {
}
