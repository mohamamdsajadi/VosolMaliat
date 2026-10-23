package com.mparsict.vosolmaliat.application.dto.response;

/**
 * نمای خلاصه برگ اجرا برای فهرست و جست‌وجو.
 *
 * @param id شناسه داخلی برگ اجرا
 * @param uniqueCode کد یکتای برگ اجرا
 * @param taxpayerName نام مؤدی
 * @param taxpayerNationalId شناسه ملی یا کد ملی مؤدی
 * @param taxpayerType نوع مؤدی
 * @param notificationDate تاریخ ابلاغ
 * @param paymentDeadlineStatus وضعیت مهلت پرداخت
 * @param totalDebt مجموع بدهی
 * @param notificationStatus وضعیت ابلاغ
 * @param currentState وضعیت جاری برگ اجرا
 * @param taxpayerCode شناسه مؤدی (TIN)
 * @param taxType عنوان فارسی نوع برگ از جدول paper_type
 * @param taxYear سال مالیاتی برگ بدهی قطعی
 * @param taxPeriod شماره دوره مالیاتی برگ بدهی قطعی
 */
public record ExecutionPaperDto(
        Long id,
        String uniqueCode,
        String taxpayerName,
        String taxpayerNationalId,
        String taxpayerType,
        String notificationDate,
        String paymentDeadlineStatus,
        @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.mparsict.vosolmaliat.domain.model.sanim.ExactDecimalSerializer.class)
        java.math.BigDecimal totalDebt,
        String notificationStatus,
        String currentState,
        java.time.LocalDateTime issuanceDate,
        String taxpayerCode,
        String taxType,
        Integer taxYear,
        Integer taxPeriod
) {}
