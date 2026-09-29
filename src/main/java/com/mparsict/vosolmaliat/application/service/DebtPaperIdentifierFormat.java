package com.mparsict.vosolmaliat.application.service;

import java.util.regex.Pattern;

/** Configurable formats used to infer and validate source identifiers. */
public enum DebtPaperIdentifierFormat {
    NATURAL_PERSON("[0-9]{10}"),
    LEGAL_PERSON("[0-9]{11}"),
    TAX_YEAR("[0-9]{4}");

    private final Pattern pattern;

    DebtPaperIdentifierFormat(String regex) {
        this.pattern = Pattern.compile(regex);
    }

    public boolean matches(String value) {
        return value != null && pattern.matcher(value).matches();
    }
}
