package com.mparsict.vosolmaliat.domain.enums;

public enum TaxSourceType implements LookupValue {
    VAT(1, "مالیات بر ارزش افزوده"),
    INCOME_TAX(2, "مالیات بر درآمد"),
    CORPORATE_TAX(3, "مالیات اشخاص حقوقی"),
    SALARY_TAX(4, "مالیات حقوق"),
    DIRECT_TAX(5, "مالیات مستقیم"),
    PROPERTY_TAX(6, "مالیات املاک"),
    OTHER(7, "سایر منابع مالیاتی");

    private final int id;
    private final String persianLabel;

    TaxSourceType(int id, String persianLabel) {
        this.id = id;
        this.persianLabel = persianLabel;
    }

    @Override public int id() { return id; }
    @Override public String label() { return persianLabel; }

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
