package com.mparsict.vosolmaliat.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/** Canonical SANIM paper types. Source aliases are normalized at the boundary. */
public enum PaperType {
    EF03("برگ اجرایی"),
    EF23("برگ قطعی مالیات و عوارض ارزش افزوده"),
    EF23_1("برگ قطعی مالیات و عوارض ارزش افزوده ـ نوع ۱"),
    EF23_2("برگ قطعی مالیات و عوارض ارزش افزوده ـ نوع ۲"),
    EF104("برگ قطعی جرائم مالیاتی");

    private final String persianTitle;

    PaperType(String persianTitle) {
        this.persianTitle = persianTitle;
    }

    @JsonValue
    public String code() {
        return name();
    }

    public String getPersianTitle() {
        return persianTitle;
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
