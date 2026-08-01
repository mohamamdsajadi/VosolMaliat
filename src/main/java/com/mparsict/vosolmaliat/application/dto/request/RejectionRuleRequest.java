package com.mparsict.vosolmaliat.application.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * اطلاعات قانون رد صدور برگ اجرا.
 *
 * @param ruleJson تعریف JSON قانون رد
 * @param startDate تاریخ شروع اعتبار قانون
 * @param endDate تاریخ پایان اعتبار قانون
 * @param documentNo شماره مستند قانونی
 * @param filePath مسیر فایل مستند
 * @param description توضیح قانون
 */
public record RejectionRuleRequest(
        @NotBlank String ruleJson,
        @NotBlank String startDate,
        @NotBlank String endDate,
        String documentNo,
        String filePath,
        String description
) {}
