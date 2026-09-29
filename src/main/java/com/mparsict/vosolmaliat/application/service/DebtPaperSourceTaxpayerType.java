package com.mparsict.vosolmaliat.application.service;

import com.mparsict.vosolmaliat.domain.enums.TaxpayerType;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Accepted XML values for each canonical taxpayer type. */
public enum DebtPaperSourceTaxpayerType {
    NATURAL(TaxpayerType.NATURAL, "1", "NATURAL", "حقیقی", "شخص حقیقی"),
    LEGAL(TaxpayerType.LEGAL, "2", "LEGAL", "حقوقی", "شخص حقوقی");

    private final TaxpayerType taxpayerType;
    private final Set<String> acceptedValues;

    DebtPaperSourceTaxpayerType(TaxpayerType taxpayerType, String... acceptedValues) {
        this.taxpayerType = taxpayerType;
        this.acceptedValues = Arrays.stream(acceptedValues)
                .map(DebtPaperSourceTaxpayerType::normalize)
                .collect(Collectors.toUnmodifiableSet());
    }

    public TaxpayerType taxpayerType() {
        return taxpayerType;
    }

    public static DebtPaperSourceTaxpayerType fromValue(String value) {
        String normalized = normalize(value);
        return Arrays.stream(values())
                .filter(type -> type.acceptedValues.contains(normalized))
                .findFirst()
                .orElseThrow(() -> SanimXmlValues.invalid("Unresolved taxpayer type in XML."));
    }

    private static String normalize(String value) {
        return value.replace('ي', 'ی').trim().toUpperCase(Locale.ROOT);
    }
}
