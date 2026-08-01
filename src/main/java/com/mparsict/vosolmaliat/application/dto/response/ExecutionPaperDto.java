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
 */
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
