package com.mparsict.vosolmaliat.domain.enums;

public enum ExecutionPaperState {
    UNASSIGNED,               // تخصیص نیافته
    ASSIGNED,                 // تخصیص یافته
    NOTIFIED,                 // ابلاغ شده
    NOTIFICATION_FAILED,      // عدم ابلاغ
    AWAITING_ARREST_ORDER,    // در انتظار صدور دستور بازداشت
    ARREST_ORDER_ISSUED,      // دستور بازداشت صادر شده
    ARREST_ORDER_NOT_ISSUED,  // دستور بازداشت صادر نشده
    COMPLETED,                // تکمیل شده
    CANCELLED                 // لغو شده
}
