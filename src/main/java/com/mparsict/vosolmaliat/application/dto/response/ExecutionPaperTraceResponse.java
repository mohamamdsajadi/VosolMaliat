package com.mparsict.vosolmaliat.application.dto.response;

import java.util.List;

/**
 * تاریخچه فرایندی و عملیاتی برگ اجرا.
 *
 * @param executionPaperId شناسه داخلی برگ اجرا
 * @param executionPaperUniqueCode کد یکتای برگ اجرا
 * @param items رویدادهای تاریخچه به ترتیب زمانی
 */
public record ExecutionPaperTraceResponse(
        Long executionPaperId,
        String executionPaperUniqueCode,
        List<ExecutionPaperTraceItemResponse> items
) {}
