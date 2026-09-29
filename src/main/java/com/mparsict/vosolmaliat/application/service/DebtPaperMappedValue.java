package com.mparsict.vosolmaliat.application.service;

/**
 * Fixed output values selected by a paper mapping profile.
 *
 * <p>Both the persisted value and its human label live here, so changing a
 * configured value does not require editing the mapper.</p>
 */
public enum DebtPaperMappedValue {
    DEFAULT_TAX_PERIOD("1", "دوره پیش‌فرض"),
    SANIM_TAXPAYER_ID("SANIM_TAXPAYER_ID", "شناسه مؤدی سنیم"),
    ECONOMIC_NUMBER("ECONOMIC_NUMBER", "شماره اقتصادی"),
    IRR("IRR", "ریال ایران"),
    GROSS_REPORTED("GROSS_REPORTED", "مبلغ ناخالص اعلام‌شده"),
    NET_REPORTED("NET_REPORTED", "مبلغ خالص اعلام‌شده"),
    NOT_REPORTED("NOT_REPORTED", "اعلام‌نشده"),
    COMBINED_EXEMPTION_REMISSION("COMBINED_EXEMPTION_REMISSION", "معافیت و بخشودگی تجمیعی"),
    REPORTED("REPORTED", "اعلام‌شده"),
    VALIDATED("VALIDATED", "اعتبارسنجی‌شده"),
    VALIDATED_WITH_PROVISIONAL_MAPPING(
            "VALIDATED_WITH_PROVISIONAL_MAPPING",
            "اعتبارسنجی‌شده با نگاشت موقت"),
    FIDA("FIDA", "شناسه فراگیر اتباع خارجی"),
    NATIONAL_ID("NATIONAL_ID", "شناسه ملی شخص حقوقی"),
    NATIONAL_CODE("NATIONAL_CODE", "کد ملی شخص حقیقی");

    private final String value;
    private final String label;

    DebtPaperMappedValue(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String value() {
        return value;
    }

    public String label() {
        return label;
    }
}
