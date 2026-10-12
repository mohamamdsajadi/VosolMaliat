package com.mparsict.vosolmaliat.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/** Canonical SANIM paper types. Source aliases are normalized at the boundary. */
public enum PaperType implements LookupValue {
    EF03(1, "برگ اجرایی", TaxSourceType.INCOME_TAX),
    EF23(2, "برگ قطعی مالیات و عوارض ارزش افزوده", TaxSourceType.VAT),
    EF23_1(3, "برگ قطعی مالیات و عوارض ارزش افزوده ـ نوع ۱", TaxSourceType.VAT),
    EF23_2(4, "برگ قطعی مالیات و عوارض ارزش افزوده ـ نوع ۲", TaxSourceType.VAT),
    EF104(5, "برگ قطعی جرائم مالیاتی", TaxSourceType.DIRECT_TAX);

    private final int id;
    private final String persianTitle;
    private final TaxSourceType taxSource;

    PaperType(int id, String persianTitle, TaxSourceType taxSource) {
        this.id = id;
        this.persianTitle = persianTitle;
        this.taxSource = taxSource;
    }

    @Override public int id() { return id; }
    @Override public String label() { return persianTitle; }

    @JsonValue
    public String code() {
        return name();
    }

    public String getPersianTitle() {
        return persianTitle;
    }

    public TaxSourceType taxSource() {
        return taxSource;
    }

    public String sourceCode() {
        return "LTR_" + name();
    }

    @JsonCreator
    public static PaperType fromCode(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("paperType is required");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("LTR_")) {
            normalized = normalized.substring(4);
        }
        return PaperType.valueOf(normalized);
    }
}
