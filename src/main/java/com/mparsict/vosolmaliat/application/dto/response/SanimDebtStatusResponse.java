package com.mparsict.vosolmaliat.application.dto.response;

import com.mparsict.vosolmaliat.domain.enums.TaxpayerType;

import java.time.LocalDateTime;

public record SanimDebtStatusResponse(
        TaxpayerType taxpayerType, String taxpayerIdentifier, String taxpayerDisplayName,
        long totalDebtRials, long totalPenaltyRials, long totalPaymentRials, long totalCreditRials,
        long outstandingDebtRials, int entryCount, LocalDateTime lastActivityAt,
        com.mparsict.vosolmaliat.domain.model.sanim.SanimAmounts amounts
) {
    public SanimDebtStatusResponse(TaxpayerType taxpayerType, String taxpayerIdentifier, String taxpayerDisplayName,
            long totalDebtRials, long totalPenaltyRials, long totalPaymentRials, long totalCreditRials,
            long outstandingDebtRials, int entryCount, LocalDateTime lastActivityAt) {
        this(taxpayerType, taxpayerIdentifier, taxpayerDisplayName, totalDebtRials, totalPenaltyRials,
                totalPaymentRials, totalCreditRials, outstandingDebtRials, entryCount, lastActivityAt, null);
    }
}
