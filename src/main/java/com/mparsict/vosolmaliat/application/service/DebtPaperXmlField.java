package com.mparsict.vosolmaliat.application.service;

/**
 * Logical fields used by a debt-paper mapping profile.
 *
 * <p>The label is kept beside the field so configuration screens and validation
 * messages do not need a second field-name lookup table.</p>
 */
public enum DebtPaperXmlField {
    DOCUMENT_NUMBER("شماره برگ"),
    ISSUE_DATE("تاریخ صدور"),
    BASIS_DOCUMENT_NUMBER("شماره مدرک مبنا"),
    BASIS_DOCUMENT_DATE("تاریخ مدرک مبنا"),
    ISSUANCE_REASON("علت صدور"),
    TAX_YEAR("سال عملکرد"),
    TAX_PERIOD("دوره عملکرد"),
    TAX_PERIOD_START("تاریخ شروع دوره"),
    TAX_PERIOD_END("تاریخ پایان دوره"),
    TAXPAYER_IDENTIFIER("شناسه مؤدی در سنیم"),
    PERSON_TYPE("نوع شخص"),
    IDENTITY_NUMBER("شناسه هویتی"),
    TAXPAYER_NAME("نام مؤدی"),
    TRADE_NAME("نام تجاری"),
    ACTIVITY_TITLE("عنوان فعالیت"),
    PHONE("تلفن"),
    POSTAL_CODE("کد پستی"),
    ADDRESS("نشانی"),
    GENERAL_DIRECTORATE_NAME("نام اداره کل"),
    TAX_OFFICE_NAME("نام اداره امور مالیاتی");

    private final String label;

    DebtPaperXmlField(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
