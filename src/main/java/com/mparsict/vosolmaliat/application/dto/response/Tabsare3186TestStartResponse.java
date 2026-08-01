package com.mparsict.vosolmaliat.application.dto.response;

/**
 * نتیجه شروع نمونه آزمایشی فرایند تبصره ۳ ماده ۱۸۶.
 *
 * @param processInstanceKey کلید نمونه فرایند Camunda
 * @param officialGazetteRequestId شناسه درخواست روزنامه رسمی
 * @param message پیام نتیجه شروع فرایند
 */
public record Tabsare3186TestStartResponse(
        Long processInstanceKey,
        String officialGazetteRequestId,
        String message
) {
}
