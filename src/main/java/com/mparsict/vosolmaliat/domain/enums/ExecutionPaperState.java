package com.mparsict.vosolmaliat.domain.enums;

public enum ExecutionPaperState implements LookupValue {
    UNASSIGNED(1, "تخصیص نیافته"),
    ASSIGNED(2, "تخصیص یافته"),
    INFORM_SENT(3, "ارسال شده برای ابلاغ"),
    INFORMED(4, "ابلاغ شده"),
    INFORM_REJECTED(5, "رد شده در ابلاغ"),
    NOTIFIED(6, "ابلاغ شده"),
    NOTIFICATION_FAILED(7, "عدم ابلاغ"),
    AWAITING_ARREST_ORDER(8, "در انتظار صدور دستور بازداشت"),
    ARREST_ORDER_ISSUED(9, "دستور بازداشت صادر شده"),
    ARREST_ORDER_NOT_ISSUED(10, "دستور بازداشت صادر نشده"),
    COMPLETED(11, "تکمیل شده"),
    CANCELLED(12, "لغو شده");

    private final int id;
    private final String persianLabel;

    ExecutionPaperState(int id, String persianLabel) {
        this.id = id;
        this.persianLabel = persianLabel;
    }

    @Override public int id() { return id; }
    @Override public String label() { return persianLabel; }

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
