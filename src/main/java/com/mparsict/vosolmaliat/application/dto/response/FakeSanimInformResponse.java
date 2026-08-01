package com.mparsict.vosolmaliat.application.dto.response;

import java.time.LocalDateTime;

/**
 * داده ابلاغ ارسال‌شده به سامانه عملیاتی آزمایشی.
 *
 * @param id شناسه داخلی رکورد آزمایشی
 * @param executionPaperId شناسه داخلی برگ اجرا
 * @param executionPaperCode کد یکتای برگ اجرا
 * @param executionPaperCreatedAt زمان ایجاد برگ اجرا
 * @param messageType نوع پیام ابلاغ یا رد
 * @param debtPaperId شناسه برگ بدهی
 * @param status وضعیت ارسال
 * @param informDate تاریخ ابلاغ
 * @param rejectionReasonCode کد علت رد ابلاغ
 * @param rejectionReasonDescription شرح علت رد ابلاغ
 * @param restrictionRemovalDate تاریخ رفع محدودیت
 * @param sentAt زمان ارسال
 * @param confirmedAt زمان تأیید
 * @param errorMessage پیام خطای آخرین عملیات
 */
public record FakeSanimInformResponse(
        Long id,
        Long executionPaperId,
        String executionPaperCode,
        LocalDateTime executionPaperCreatedAt,
        String messageType,
        String debtPaperId,
        String status,
        String informDate,
        String rejectionReasonCode,
        String rejectionReasonDescription,
        String restrictionRemovalDate,
        String sentAt,
        String confirmedAt,
        String errorMessage
) {}
