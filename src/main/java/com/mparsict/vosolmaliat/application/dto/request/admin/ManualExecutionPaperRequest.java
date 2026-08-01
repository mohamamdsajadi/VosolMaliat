package com.mparsict.vosolmaliat.application.dto.request.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * اطلاعات ایجاد دستی برگ اجرا توسط مدیر سامانه.
 *
 * @param executionPaperUniqueCode کد یکتای اختیاری برگ اجرا
 * @param finalizedDebtPaperId شناسه برگ بدهی قطعی
 * @param taxpayerId شناسه مؤدی
 * @param mainTaxBureauId شناسه حوزه مالیاتی اصلی
 * @param taxBureauId شناسه حوزه مالیاتی
 * @param taxUnitId شناسه واحد مالیاتی
 * @param sendingUnitId شناسه واحد ارسال‌کننده
 * @param finalizationReason علت قطعی شدن بدهی
 * @param taxSource منبع مالیاتی
 * @param currentState وضعیت اولیه برگ اجرا
 * @param debtInfo اجزای مالی بدهی
 */
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
    /**
     * مبالغ بدهی برای ثبت دستی برگ اجرا.
     *
     * @param taxAmount اصل مالیات
     * @param dutyAmount عوارض
     * @param penaltyArticle22 جریمه ماده ۲۲
     * @param penaltyArticle23 جریمه ماده ۲۳
     * @param deductiblePenalty جریمه قابل بخشودگی
     * @param nonDeductiblePenalty جریمه غیرقابل بخشودگی
     * @param taxPaidAmount مبلغ مالیات پرداخت‌شده
     * @param penaltyPaidAmount مبلغ جریمه پرداخت‌شده
     * @param taxCredit اعتبار مالیاتی
     * @param remainingTax مانده مالیات
     * @param municipalityShare سهم شهرداری
     * @param guildShare سهم اتحادیه یا صنف
     * @param complianceBonus جایزه خوش‌حسابی
     */
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
