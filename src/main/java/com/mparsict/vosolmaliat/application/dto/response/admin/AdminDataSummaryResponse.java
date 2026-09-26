package com.mparsict.vosolmaliat.application.dto.response.admin;

import java.util.List;

/**
 * خلاصه داده‌های مرجع موردنیاز ایجاد دستی برگ اجرا.
 *
 * @param finalizedDebtPapers برگ‌های بدهی قطعی قابل انتخاب
 * @param taxpayers مؤدیان قابل انتخاب
 * @param executionPapers برگ‌های اجرای موجود
 */
public record AdminDataSummaryResponse(
        List<FinalizedDebtPaperSummary> finalizedDebtPapers,
        List<TaxpayerSummary> taxpayers,
        List<ExecutionPaperSummary> executionPapers
) {
    /**
     * خلاصه برگ بدهی قطعی.
     *
     * @param id شناسه داخلی برگ بدهی
     * @param debtPaperUniqueCode کد یکتای برگ بدهی
     * @param taxSource منبع مالیاتی
     */
    public record FinalizedDebtPaperSummary(
            Long id,
            String debtPaperUniqueCode,
            String taxSource
    ) {}

    /**
     * خلاصه مؤدی متصل به برگ بدهی.
     *
     * @param id شناسه داخلی مؤدی
     * @param tin شناسه مؤدی
     * @param registrationTraceCode کد رهگیری ثبت‌نام
     * @param type نوع مؤدی
     */
    public record TaxpayerSummary(
            Long id,
            String tin,
            String registrationTraceCode,
            String type
    ) {}

    /**
     * خلاصه برگ اجرای موجود.
     *
     * @param id شناسه داخلی برگ اجرا
     * @param executionPaperUniqueCode کد یکتای برگ اجرا
     * @param currentState وضعیت جاری برگ اجرا
     */
    public record ExecutionPaperSummary(
            Long id,
            String executionPaperUniqueCode,
            String currentState
    ) {}
}
