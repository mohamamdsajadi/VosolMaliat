package com.mparsict.vosolmaliat.domain.enums;

import java.util.Locale;

/** علت قطعیت and علت صدور denote the same business value. */
public enum FinalizationReason {
    SELF_DECLARATION("خوداظهاری"),
    FINAL_NOTICE("برگه بدهی قطعی");

    private final String label;

    FinalizationReason(String label) {
        this.label = label;
    }

    public String label() {
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
