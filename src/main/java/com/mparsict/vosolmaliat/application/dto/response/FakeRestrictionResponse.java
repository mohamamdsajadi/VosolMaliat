package com.mparsict.vosolmaliat.application.dto.response;

/**
 * نتیجه اعمال محدودیت در سامانه آزمایشی.
 *
 * @param id شناسه داخلی رکورد آزمایشی
 * @param officialGazetteRequestId شناسه درخواست مبدأ روزنامه رسمی
 * @param executionPaperId شناسه داخلی برگ اجرا
 * @param executionPaperCode کد یکتای برگ اجرا
 * @param status وضعیت اعمال محدودیت
 * @param relatedPersonCount تعداد اشخاص مشمول محدودیت
 * @param payload محتوای ارسالی محدودیت
 * @param sentAt زمان ارسال
 * @param confirmedAt زمان تأیید
 * @param errorMessage پیام خطای آخرین عملیات
 */
public record FakeRestrictionResponse(
        Long id,
        String officialGazetteRequestId,
        Long executionPaperId,
        String executionPaperCode,
        String status,
        Integer relatedPersonCount,
        String payload,
        String sentAt,
        String confirmedAt,
        String errorMessage
) {
}
