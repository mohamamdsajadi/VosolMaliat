package com.mparsict.vosolmaliat.application.dto.response;

public record FakeElectronicNotificationResponse(
        Long id,
        Long executionPaperId,
        String executionPaperCode,
        String externalTrackingCode,
        String status,
        String sentAt,
        String autoConfirmAt,
        String informDate,
        String confirmedAt,
        String callbackDeliveredAt,
        String errorMessage
) {}
