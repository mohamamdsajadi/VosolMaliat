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
        @NotBlank(message = "شماره سند الزامی است") String documentNo,
        @NotBlank(message = "آدرس فایل سند الزامی است") String filePath,
        String description,
        @NotBlank(message = "کد خطا الزامی است") String errorCode,
        @NotBlank(message = "متن خطا الزامی است") String errorMessage,
        Long parentRuleId
) {
    public RejectionRuleRequest(String json, String start, String end, String document, String path, String description) {
        this(json, start, end, document, path, description, null, null, null);
    }
}
