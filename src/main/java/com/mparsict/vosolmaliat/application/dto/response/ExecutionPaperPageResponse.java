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
 */
public record ExecutionPaperPageResponse(
        List<ExecutionPaperDto> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages
) {}
