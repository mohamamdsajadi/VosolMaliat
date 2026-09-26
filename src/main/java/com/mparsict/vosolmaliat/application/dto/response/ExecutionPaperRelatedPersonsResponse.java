package com.mparsict.vosolmaliat.application.dto.response;

import java.util.List;

/**
 * اشخاص مرتبط با مؤدی یک برگ اجرا.
 *
 * @param executionPaperId شناسه داخلی برگ اجرا
 * @param ownerLegalPersonId شناسه داخلی شخص حقوقی مؤدی
 * @param ownerNationalId شناسه ملی شخص حقوقی مؤدی
 * @param debtYear سال بدهی مبنای تشخیص اشخاص مرتبط
 * @param lastSyncedAt زمان آخرین همگام‌سازی
 * @param relatedPersons اشخاص مرتبط دریافت‌شده
 */
public record ExecutionPaperRelatedPersonsResponse(
        Long executionPaperId,
        Long ownerLegalPersonId,
        String ownerNationalId,
        String debtYear,
        String lastSyncedAt,
        List<RelatedPersonResponse> relatedPersons
) {
}
