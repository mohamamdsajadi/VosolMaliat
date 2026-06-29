package com.mparsict.vosolmaliat.common.exception;

import lombok.Getter;

@Getter
public class RuleViolationException extends BusinessException {

    private final String rejectionReasonCode;
    private final String rejectionReasonDescription;
    private final String restrictionRemovalDate;

    public RuleViolationException(
            String rejectionReasonCode,
            String rejectionReasonDescription,
            String restrictionRemovalDate) {
        super("RULE_VIOLATION", "درخواست به دلیل نقض قوانین تعریف‌شده رد شد");
        this.rejectionReasonCode = rejectionReasonCode;
        this.rejectionReasonDescription = rejectionReasonDescription;
        this.restrictionRemovalDate = restrictionRemovalDate;
    }
}
