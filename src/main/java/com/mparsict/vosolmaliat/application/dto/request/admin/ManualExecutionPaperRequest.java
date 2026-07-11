package com.mparsict.vosolmaliat.application.dto.request.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ManualExecutionPaperRequest(
        String executionPaperUniqueCode,
        @NotNull Long finalizedDebtPaperId,
        @NotNull Long taxpayerId,
        Long mainTaxBureauId,
        Long taxBureauId,
        Long taxUnitId,
        Long sendingUnitId,
        @NotBlank String finalizationReason,
        @NotBlank String taxSource,
        String currentState,
        @Valid DebtInformationInput debtInfo
) {
    public record DebtInformationInput(
            Double taxAmount,
            Double dutyAmount,
            Double penaltyArticle22,
            Double penaltyArticle23,
            Double deductiblePenalty,
            Double nonDeductiblePenalty,
            Double taxPaidAmount,
            Double penaltyPaidAmount,
            Double taxCredit,
            Double remainingTax,
            Double municipalityShare,
            Double guildShare,
            Double complianceBonus
    ) {}
}
