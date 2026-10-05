package com.mparsict.vosolmaliat.application.dto.response;

import java.util.Map;

/**
 * جزئیات کامل برگ اجرا و اطلاعات وابسته آن.
 *
 * @param summary خلاصه قابل نمایش برگ اجرا
 * @param executionPaper مشخصات خام برگ اجرا
 * @param finalTaxDebtPaper مشخصات برگ بدهی قطعی
 * @param taxpayer مشخصات مؤدی
 * @param debtInformation اجزای مالی بدهی
 * @param notification اطلاعات ابلاغ
 * @param ruleRejection اطلاعات رد مبتنی بر قانون
 */
public record ExecutionPaperDetailResponse(
        ExecutionPaperDto summary,
        Map<String, Object> executionPaper,
        Map<String, Object> finalTaxDebtPaper,
        Map<String, Object> taxpayer,
        Map<String, Object> debtInformation,
        Map<String, Object> notification,
        Map<String, Object> ruleRejection
) {}
