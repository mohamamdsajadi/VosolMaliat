package com.mparsict.vosolmaliat.application.service;

import com.mparsict.vosolmaliat.domain.model.sanim.*;

public final class SanimModelValidation {
    private SanimModelValidation() {}
    public static void requireDocument(SanimDocumentDetails document) {
        if (document == null || "LEGACY_REVIEW_REQUIRED".equals(document.getValidationStatus()))
            throw SanimXmlValues.invalid("Source document requires review before processing.");
        requireAmounts(document.getAmounts());
        if ("LTR_EF03".equals(document.getTemplateCode())) {
            if (document.getRegistrationTraceCode() == null || document.getRegistrationTraceCode().isBlank())
                throw SanimXmlValues.invalid("Registration followCode is required.");
            if (document.getTaxpayerIdentifier() == null || !document.getTaxpayerIdentifier().matches("[0-9]+"))
                throw SanimXmlValues.invalid("Numeric taxpayer TIN is required.");
            if (document.getBasisDocumentNumber() == null || document.getBasisDocumentNumber().isBlank()
                    || document.getBasisDocumentDateRaw() == null || document.getBasisDocumentDateRaw().isBlank())
                throw SanimXmlValues.invalid("EF03 basis document number and date are required.");
            if (document.getTaxPeriodStartRaw() == null || document.getTaxPeriodEndRaw() == null)
                throw SanimXmlValues.invalid("EF03 XML period start and end are required.");
        }
        if (document.getDocumentNumber() == null || document.getDocumentNumber().isBlank()
                || document.getIssueDateRaw() == null || document.getIssueDateRaw().isBlank())
            throw SanimXmlValues.invalid("Source document number and date are required.");
    }
    public static void requireAmounts(SanimAmounts a) {
        if (a == null || a.getOutstandingDebtAmount() == null || a.getTotalTaxAmount() == null
                || a.getTotalDutyAmount() == null || a.getTotalPenaltyAmount() == null
                || a.getTotalPaidAmount() == null || a.getTotalCreditAmount() == null || a.getTotalRemissionAmount() == null)
            throw SanimXmlValues.invalid("All seven aggregate amounts must be supplied; missing is not zero in manual entry.");
    }
    static Object amount(String field, SanimAmounts a) {
        if (a == null) throw SanimXmlValues.invalid("Current amounts require review.");
        return switch (field) {
            case "debtAmount", "outstandingDebtAmount" -> a.getOutstandingDebtAmount();
            case "totalTaxAmount" -> a.getTotalTaxAmount();
            case "totalDutyAmount" -> a.getTotalDutyAmount();
            case "totalPenaltyAmount" -> a.getTotalPenaltyAmount();
            case "totalPaidAmount" -> a.getTotalPaidAmount();
            case "totalCreditAmount" -> a.getTotalCreditAmount();
            case "totalRemissionAmount" -> a.getTotalRemissionAmount();
            default -> throw SanimXmlValues.invalid("Unsupported or removed rule field requires review: " + field);
        };
    }
}
