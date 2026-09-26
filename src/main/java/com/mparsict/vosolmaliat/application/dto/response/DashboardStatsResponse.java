package com.mparsict.vosolmaliat.application.dto.response;

/**
 * آمار تجمیعی برگ‌های اجرا برای داشبورد.
 *
 * @param notInformedExecutionPapers تعداد برگ‌های اجرای ابلاغ‌نشده
 * @param informedExecutionPapers تعداد برگ‌های اجرای ابلاغ‌شده
 * @param activeExecutionPapers تعداد برگ‌های اجرای فعال
 * @param rejectedDebtPapers تعداد برگ‌های بدهی ردشده
 * @param totalIssuedExecutionPapers تعداد کل برگ‌های اجرای صادرشده
 * @param unassignedExecutionPapers تعداد برگ‌های اجرای تخصیص‌نیافته
 * @param arrestOrderIssued تعداد برگ‌های دارای دستور توقیف
 * @param arrestOrderNotIssued تعداد برگ‌های بدون دستور توقیف
 * @param awaitingArrestOrder تعداد برگ‌های در انتظار دستور توقیف
 * @param debtSumNotInformedExecutionPapers مجموع بدهی برگ‌های ابلاغ‌نشده
 * @param debtSumInformedExecutionPapers مجموع بدهی برگ‌های ابلاغ‌شده
 * @param debtSumRejectedDebtPapers مجموع بدهی برگ‌های ردشده
 * @param debtSumTotalIssuedExecutionPapers مجموع بدهی کل برگ‌های صادرشده
 * @param myActionsNew تعداد اقدامات جدید کاربر
 * @param myActionsAwaitingCompletion تعداد اقدامات در انتظار تکمیل کاربر
 * @param myActionsCompleted تعداد اقدامات تکمیل‌شده کاربر
 * @param myActionsCancelled تعداد اقدامات لغوشده کاربر
 * @param totalDebtAwaitingSeizure مجموع بدهی در انتظار توقیف
 * @param totalDebtWithSeizureOrder مجموع بدهی دارای دستور توقیف
 * @param collectedAmounts مجموع مبالغ وصول‌شده
 * @param uncollectedAmounts مجموع مبالغ وصول‌نشده
 */
public record DashboardStatsResponse(
        // Execution paper report (for pie chart)
        long notInformedExecutionPapers,
        long informedExecutionPapers,
        long activeExecutionPapers,
        long rejectedDebtPapers,
        long totalIssuedExecutionPapers,
        // Assignment report (for pie chart)
        long unassignedExecutionPapers,
        long arrestOrderIssued,
        long arrestOrderNotIssued,
        long awaitingArrestOrder,
        // Debt totals aligned with pie chart groups
        @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.mparsict.vosolmaliat.domain.model.sanim.ExactDecimalSerializer.class)
        java.math.BigDecimal debtSumNotInformedExecutionPapers,
        @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.mparsict.vosolmaliat.domain.model.sanim.ExactDecimalSerializer.class)
        java.math.BigDecimal debtSumInformedExecutionPapers,
        @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.mparsict.vosolmaliat.domain.model.sanim.ExactDecimalSerializer.class)
        java.math.BigDecimal debtSumRejectedDebtPapers,
        @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.mparsict.vosolmaliat.domain.model.sanim.ExactDecimalSerializer.class)
        java.math.BigDecimal debtSumTotalIssuedExecutionPapers,
        // My actions
        long myActionsNew,
        long myActionsAwaitingCompletion,
        long myActionsCompleted,
        long myActionsCancelled,
        // Financial collection status
        @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.mparsict.vosolmaliat.domain.model.sanim.ExactDecimalSerializer.class)
        java.math.BigDecimal totalDebtAwaitingSeizure,
        @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.mparsict.vosolmaliat.domain.model.sanim.ExactDecimalSerializer.class)
        java.math.BigDecimal totalDebtWithSeizureOrder,
        @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.mparsict.vosolmaliat.domain.model.sanim.ExactDecimalSerializer.class)
        java.math.BigDecimal collectedAmounts,
        @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.mparsict.vosolmaliat.domain.model.sanim.ExactDecimalSerializer.class)
        java.math.BigDecimal uncollectedAmounts
) {}
