package com.mparsict.vosolmaliat.domain.enums;

public enum TaxpayerType implements LookupValue {
    LEGAL(1, "حقوقی"),
    NATURAL(2, "حقیقی"),
    PARTNERSHIP(3, "حقیقی مشارکتی");

    private final int id;
    private final String persianLabel;

    TaxpayerType(int id, String persianLabel) {
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
