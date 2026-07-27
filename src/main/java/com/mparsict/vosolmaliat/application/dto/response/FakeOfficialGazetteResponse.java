package com.mparsict.vosolmaliat.application.dto.response;

public record FakeOfficialGazetteResponse(
        Long id,
        String requestId,
        Long executionPaperId,
        String executionPaperCode,
        String legalPersonNationalId,
        String debtPaperDate,
        String status,
        String sentAt,
        String respondedAt,
        Integer relatedPersonCount,
        String errorMessage
) {
}
