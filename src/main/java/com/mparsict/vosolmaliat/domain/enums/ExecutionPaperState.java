package com.mparsict.vosolmaliat.domain.enums;

public enum ExecutionPaperState {
    UNASSIGNED("تخصیص نیافته"),
    ASSIGNED("تخصیص یافته"),
    NOTIFIED("ابلاغ شده"),
    NOTIFICATION_FAILED("عدم ابلاغ"),
    AWAITING_ARREST_ORDER("در انتظار صدور دستور بازداشت"),
    ARREST_ORDER_ISSUED("دستور بازداشت صادر شده"),
    ARREST_ORDER_NOT_ISSUED("دستور بازداشت صادر نشده"),
    COMPLETED("تکمیل شده"),
    CANCELLED("لغو شده");

    private final String persianLabel;

    ExecutionPaperState(String persianLabel) {
        this.persianLabel = persianLabel;
    }

    public String getPersianLabel() {
        return persianLabel;
    }

    public static String labelOf(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return valueOf(code).getPersianLabel();
        } catch (IllegalArgumentException ex) {
            return code;
        }
    }

    public static String displayOf(String code) {
        String label = labelOf(code);
        if (label == null) {
            return null;
        }
        return label.equals(code) ? code : label + " (" + code + ")";
    }
}
