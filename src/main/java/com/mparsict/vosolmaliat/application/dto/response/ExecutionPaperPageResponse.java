package com.mparsict.vosolmaliat.application.dto.response;

import java.util.List;

/**
 * صفحه نتایج جست‌وجوی برگ‌های اجرا.
 *
 * @param content برگ‌های اجرای صفحه جاری
 * @param pageNumber شماره صفحه
 * @param pageSize تعداد رکورد هر صفحه
 * @param totalElements تعداد کل رکوردها
 * @param totalPages تعداد کل صفحات
 * @param lastViewedAt زمان آخرین مشاهده کاربر جاری، فقط برای برگ‌های همین صفحه
 */
public record ExecutionPaperPageResponse(
        List<ExecutionPaperDto> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        java.util.Map<Long, java.time.Instant> lastViewedAt
) {
    public ExecutionPaperPageResponse(List<ExecutionPaperDto> content, int pageNumber, int pageSize, long totalElements, int totalPages) {
        this(content, pageNumber, pageSize, totalElements, totalPages, java.util.Map.of());
    }
}
