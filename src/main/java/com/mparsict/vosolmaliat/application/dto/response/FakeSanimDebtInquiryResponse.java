package com.mparsict.vosolmaliat.application.dto.response;

import com.mparsict.vosolmaliat.domain.enums.TaxpayerType;

import java.time.LocalDateTime;

public record FakeSanimDebtInquiryResponse(
        Long id,
        String requestId,
        Long executionPaperId,
        String executionPaperCode,
        TaxpayerType taxpayerType,
        String taxpayerIdentifier,
        String status,
        LocalDateTime sentAt,
        LocalDateTime respondedAt,
        SanimDebtStatusResponse debtStatus,
        String errorMessage
) {
}
