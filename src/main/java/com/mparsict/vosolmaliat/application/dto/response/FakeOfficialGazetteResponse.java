package com.mparsict.vosolmaliat.application.dto.response;

/**
 * درخواست یا پاسخ ثبت‌شده در روزنامه رسمی آزمایشی.
 *
 * @param id شناسه داخلی رکورد آزمایشی
 * @param requestId شناسه همبستگی درخواست روزنامه رسمی
 * @param executionPaperId شناسه داخلی برگ اجرا
 * @param executionPaperCode کد یکتای برگ اجرا
 * @param legalPersonNationalId شناسه ملی مؤدی حقوقی
 * @param debtPaperDate تاریخ صدور برگ بدهی
 * @param status وضعیت درخواست
 * @param sentAt زمان ارسال درخواست
 * @param respondedAt زمان دریافت پاسخ
 * @param relatedPersonCount تعداد اشخاص مرتبط پاسخ
 * @param errorMessage پیام خطای آخرین عملیات
 */
public record FakeOfficialGazetteResponse(
        Long id,
        String requestId,
        Long executionPaperId,
        String executionPaperCode,
        String legalPersonNationalId,
        String debtPaperDate,
        String status,
        String sentAt,
        String respondedAt,
        Integer relatedPersonCount,
        String errorMessage
) {
}
