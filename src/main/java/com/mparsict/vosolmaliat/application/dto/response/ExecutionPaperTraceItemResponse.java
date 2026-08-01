package com.mparsict.vosolmaliat.application.dto.response;

import java.time.LocalDateTime;

/**
 * یک رویداد از تاریخچه تغییرات برگ اجرا.
 *
 * @param type نوع رویداد
 * @param action کد عملیات
 * @param label عنوان فارسی عملیات
 * @param actorUsername نام کاربری عامل تغییر
 * @param oldValue مقدار پیش از تغییر
 * @param newValue مقدار پس از تغییر
 * @param occurredAt زمان وقوع رویداد
 */
public record ExecutionPaperTraceItemResponse(
        String type,
        String action,
        String label,
        String actorUsername,
        String oldValue,
        String newValue,
        LocalDateTime occurredAt
) {}
