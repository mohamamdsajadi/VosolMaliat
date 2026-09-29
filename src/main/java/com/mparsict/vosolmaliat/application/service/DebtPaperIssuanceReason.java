package com.mparsict.vosolmaliat.application.service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Configurable source values for issuance reasons that need canonical codes. */
public enum DebtPaperIssuanceReason {
    SELF_DECLARATION("SELF_DECLARATION", "خوداظهاری", "خود اظهاری"),
    FINAL_NOTICE("FINAL_NOTICE", "برگ قطعی", "برگقطعی");

    private final String code;
    private final String label;
    private final Set<String> acceptedValues;

    DebtPaperIssuanceReason(String code, String label, String... acceptedValues) {
        this.code = code;
        this.label = label;
        this.acceptedValues = Arrays.stream(acceptedValues)
                .map(DebtPaperIssuanceReason::normalize)
                .collect(Collectors.toUnmodifiableSet());
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public static DebtPaperIssuanceReason fromSourceValue(String value) {
        String normalized = normalize(value);
        return Arrays.stream(values())
                .filter(reason -> reason.acceptedValues.contains(normalized))
                .findFirst()
                .orElseThrow(() -> SanimXmlValues.invalid("Invalid finalization reason."));
    }

    private static String normalize(String value) {
        return value == null ? "" : value
                .replace('ي', 'ی')
                .replace(" ", "")
                .replace("‌", "")
                .trim();
    }
}
