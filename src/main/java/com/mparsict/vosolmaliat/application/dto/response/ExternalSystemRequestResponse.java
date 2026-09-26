package com.mparsict.vosolmaliat.application.dto.response;

public record ExternalSystemRequestResponse(
        Long id,
        String systemCode,
        String systemLabel,
        String requestType,
        String requestTypeLabel,
        String status,
        String statusLabel,
        String sentAt,
        String completedAt,
        String trackingCode,
        Object requestBody
) {}
