package com.mparsict.vosolmaliat.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * داده ورودی برگ بدهی برای بررسی و صدور برگ اجرا.
 *
 * @param debtPaperUniqueCode کد یکتای برگ بدهی
 * @param debtPaperIssueDate تاریخ صدور برگ بدهی
 * @param notificationId شناسه ابلاغ برگ بدهی
 * @param finalizationReason علت قطعی شدن بدهی
 * @param taxSource منبع مالیاتی
 * @param performanceYear سال عملکرد بدهی
 * @param performancePeriod دوره عملکرد بدهی
 * @param activityTypeCode کد نوع فعالیت مؤدی
 * @param activityTypeTitle عنوان نوع فعالیت مؤدی
 * @param mainTaxBureauId شناسه حوزه مالیاتی اصلی
 * @param taxBureauId شناسه حوزه مالیاتی
 * @param taxUnitId شناسه واحد مالیاتی
 * @param sendingUnitId شناسه واحد ارسال‌کننده
 * @param taxpayer اطلاعات هویتی مؤدی
 * @param debtInfo اجزای مالی بدهی
 * @param relatedPersons اشخاص مرتبط اعلام‌شده همراه برگ بدهی
 */
public record DebtPaperIssuanceRequest(
        @NotBlank String debtPaperUniqueCode,
        @NotBlank String debtPaperIssueDate,
        String notificationId,
        @NotBlank String finalizationReason,
        @NotBlank String taxSource,
        String performanceYear,
        String performancePeriod,
        String activityTypeCode,
        String activityTypeTitle,
        Long mainTaxBureauId,
        Long taxBureauId,
        Long taxUnitId,
        Long sendingUnitId,
        @NotNull @Valid TaxpayerInfo taxpayer,
        @NotNull @Valid DebtInfo debtInfo,
        List<RelatedPersonInfo> relatedPersons
) {

    /**
     * اطلاعات هویتی و نشانی مؤدی حقیقی یا حقوقی.
     *
     * @param type نوع مؤدی؛ حقیقی یا حقوقی
     * @param nationalCode کد ملی مؤدی حقیقی ایرانی
     * @param fida شناسه فراگیر مؤدی حقیقی خارجی
     * @param firstName نام مؤدی حقیقی
     * @param lastName نام خانوادگی مؤدی حقیقی
     * @param fatherName نام پدر مؤدی حقیقی
     * @param birthDate تاریخ تولد مؤدی حقیقی
     * @param postalCode کد پستی مؤدی
     * @param address نشانی مؤدی
     * @param nationalId شناسه ملی مؤدی حقوقی
     * @param registrationNumber شماره ثبت مؤدی حقوقی
     * @param registrationDate تاریخ ثبت مؤدی حقوقی
     * @param companyName نام مؤدی حقوقی
     */
    public record TaxpayerInfo(
            @NotBlank String type,
            // Natural person fields
            String nationalCode,
            String fida,
            String firstName,
            String lastName,
            String fatherName,
            String birthDate,
            String postalCode,
            String address,
            // Legal person fields
            String nationalId,
            String registrationNumber,
            String registrationDate,
            String companyName
    ) {}

    /**
     * مبالغ تشکیل‌دهنده بدهی و پرداخت‌های مؤدی.
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

    /**
     * مشخصات و بازه اعتبار سمت شخص مرتبط.
     *
     * @param type نوع شخص مرتبط
     * @param nationalCode کد ملی شخص حقیقی
     * @param nationalId شناسه ملی شخص حقوقی
     * @param postName عنوان سمت
     * @param startDate تاریخ شروع سمت
     * @param endDate تاریخ پایان سمت
     */
    public record RelatedPersonInfo(
            String type,
            String nationalCode,
            String nationalId,
            String postName,
            String startDate,
            String endDate
    ) {}
}
