package com.mparsict.vosolmaliat.application.dto.response;

/**
 * نتیجه ثبت‌شده در سامانه آزمایشی ابلاغ الکترونیک.
 *
 * @param id شناسه داخلی رکورد آزمایشی
 * @param executionPaperId شناسه داخلی برگ اجرا
 * @param executionPaperCode کد یکتای برگ اجرا
 * @param externalTrackingCode کد پیگیری سامانه ابلاغ
 * @param status وضعیت ارسال یا پاسخ
 * @param sentAt زمان ارسال درخواست
 * @param informDate تاریخ ابلاغ
 * @param confirmedAt زمان تأیید ابلاغ
 * @param callbackDeliveredAt زمان تحویل callback به فرایند
 * @param errorMessage پیام خطای آخرین عملیات
 */
public record FakeElectronicNotificationResponse(
        Long id,
        Long executionPaperId,
        String executionPaperCode,
        String externalTrackingCode,
        String status,
        String sentAt,
        String informDate,
        String confirmedAt,
        String callbackDeliveredAt,
        String errorMessage
) {}
