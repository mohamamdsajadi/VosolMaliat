package com.mparsict.vosolmaliat.application.dto.request;

/**
 * معیارهای فیلتر و جست‌وجوی برگ‌های اجرا.
 *
 * @param uniqueCode کد یکتای برگ اجرا
 * @param taxpayerName نام مؤدی
 * @param taxpayerNationalId شناسه ملی یا کد ملی مؤدی
 * @param currentState وضعیت جاری برگ اجرا
 * @param notificationStatus وضعیت ابلاغ
 * @param taxUnitId شناسه واحد مالیاتی
 * @param minDebt حداقل مبلغ بدهی
 * @param maxDebt حداکثر مبلغ بدهی
 */
public record ExecutionPaperSearchCriteria(
        String uniqueCode,
        String taxpayerName,
        String taxpayerNationalId,
        String currentState,
        String notificationStatus,
        Long taxUnitId,
        java.math.BigDecimal minDebt,
        java.math.BigDecimal maxDebt
) {
    /** @return معیار جست‌وجوی بدون فیلتر */
    public static ExecutionPaperSearchCriteria empty() {
        return new ExecutionPaperSearchCriteria(null, null, null, null, null, null, null, null);
    }
}
