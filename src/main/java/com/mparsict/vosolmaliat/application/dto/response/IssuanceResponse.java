package com.mparsict.vosolmaliat.application.dto.response;

/**
 * نتیجه بررسی و صدور برگ اجرا.
 *
 * @param success آیا برگ اجرا صادر شد
 * @param executionPaperUniqueCode کد یکتای برگ اجرای صادرشده
 * @param rejectionReasonCode کد علت رد
 * @param rejectionReasonDescription شرح علت رد
 * @param restrictionRemovalDate تاریخ رفع محدودیت مانع صدور
 * @param message پیام نتیجه عملیات
 */
public record IssuanceResponse(
        boolean success,
        Long executionPaperId,
        String executionPaperUniqueCode,
        String rejectionReasonCode,
        String rejectionReasonDescription,
        String restrictionRemovalDate,
        String message
) {
    public IssuanceResponse(boolean success, String executionPaperUniqueCode,
            String rejectionReasonCode, String rejectionReasonDescription,
            String restrictionRemovalDate, String message) {
        this(success, null, executionPaperUniqueCode, rejectionReasonCode,
                rejectionReasonDescription, restrictionRemovalDate, message);
    }
}
