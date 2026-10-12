package com.mparsict.vosolmaliat.domain.enums;

import java.util.Locale;

/** علت قطعیت and علت صدور denote the same business value. */
public enum FinalizationReason implements LookupValue {
    SELF_DECLARATION(0, "خوداظهاری"),
    FINAL_NOTICE(1, "برگه بدهی قطعی");

    private final int id;
    private final String label;

    FinalizationReason(int id, String label) {
        this.id = id;
        this.label = label;
    }

    @Override public int id() { return id; }

    @Override public String label() {
        return label;
    }

    public static FinalizationReason fromSourceValue(String value) {
        String normalized = value == null ? "" : value
                .replace('ي', 'ی').replace('ك', 'ک')
                .replace(" ", "").replace("‌", "").replace("\t", "").replace("\n", "").trim()
                .toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "SELF_DECLARATION", "خوداظهاری" -> SELF_DECLARATION;
            case "FINAL_NOTICE", "برگقطعی", "برگهقطعی", "برگبدهیقطعی", "برگهبدهیقطعی" -> FINAL_NOTICE;
            default -> throw new IllegalArgumentException(
                    "علت قطعیت الزامی است و باید «خوداظهاری» یا «برگه بدهی قطعی» باشد.");
        };
    }
}
