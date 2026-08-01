package com.mparsict.vosolmaliat.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * اطلاعات لازم برای شروع آزمایشی فرایند تبصره ۳ ماده ۱۸۶.
 *
 * @param executionPaperId شناسه داخلی برگ اجرا
 * @param legalPersonNationalId شناسه ملی مؤدی حقوقی
 * @param debtPaperDate تاریخ صدور برگ بدهی
 */
public record Tabsare3186TestStartRequest(
        @NotNull Long executionPaperId,
        @NotBlank String legalPersonNationalId,
        @NotBlank String debtPaperDate
) {
}
