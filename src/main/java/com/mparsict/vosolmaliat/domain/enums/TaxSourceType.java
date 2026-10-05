package com.mparsict.vosolmaliat.domain.enums;

public enum TaxSourceType {
    VAT("مالیات بر ارزش افزوده"),
    INCOME_TAX("مالیات بر درآمد"),
    CORPORATE_TAX("مالیات اشخاص حقوقی"),
    SALARY_TAX("مالیات حقوق"),
    DIRECT_TAX("مالیات مستقیم"),
    PROPERTY_TAX("مالیات املاک"),
    OTHER("سایر منابع مالیاتی");

    private final String persianLabel;

    TaxSourceType(String persianLabel) {
        this.persianLabel = persianLabel;
    }

    public String getPersianLabel() {
        return persianLabel;
    }

    public static TaxSourceType fromSourceValue(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Tax source is required");
        String normalized = value.trim().toUpperCase(java.util.Locale.ROOT);
        if ("TAXB".equals(normalized)) return INCOME_TAX;
        return valueOf(normalized);
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
