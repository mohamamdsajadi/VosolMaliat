package com.mparsict.vosolmaliat.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record DebtPaperIssuanceRequest(
        @NotBlank String debtPaperUniqueCode,
        @NotBlank String debtPaperIssueDate,
        String notificationId,
        @NotBlank String finalizationReason,
        @NotBlank String taxSource,
        String performanceYear,
        String performancePeriod,
        String activityTypeCode,
        Long mainTaxBureauId,
        Long taxBureauId,
        Long taxUnitId,
        Long sendingUnitId,
        @NotNull @Valid TaxpayerInfo taxpayer,
        @NotNull @Valid DebtInfo debtInfo,
        List<RelatedPersonInfo> relatedPersons
) {

    public record TaxpayerInfo(
            @NotBlank String type,
            // Natural person fields
            String nationalCode,
            String fida,
            String firstName,
            String lastName,
            String fatherName,
            String birthDate,
            // Legal person fields
            String nationalId,
            String registrationNumber,
            String registrationDate,
            String companyName
    ) {}

    public record DebtInfo(
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

    public record RelatedPersonInfo(
            String type,
            String nationalCode,
            String nationalId,
            String postName,
            String startDate,
            String endDate
    ) {}
}
