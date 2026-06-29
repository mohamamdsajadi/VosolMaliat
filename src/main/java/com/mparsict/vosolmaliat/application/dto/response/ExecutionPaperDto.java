package com.mparsict.vosolmaliat.application.dto.response;

public record ExecutionPaperDto(
        Long id,
        String uniqueCode,
        String taxpayerName,
        String taxpayerNationalId,
        String taxpayerType,
        String notificationDate,
        String paymentDeadlineStatus,
        Double totalDebt,
        String notificationStatus,
        String currentState
) {}
