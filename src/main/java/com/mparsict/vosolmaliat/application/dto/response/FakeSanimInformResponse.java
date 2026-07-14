package com.mparsict.vosolmaliat.application.dto.response;

public record FakeSanimInformResponse(
        Long id,
        Long executionPaperId,
        String executionPaperCode,
        String status,
        String informDate,
        String sentAt,
        String confirmedAt,
        String errorMessage
) {}
