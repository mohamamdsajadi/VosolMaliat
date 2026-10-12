package com.mparsict.vosolmaliat.domain.enums;

public enum RelatedPersonPostCode implements LookupValue {
    CEO("001", "مدیرعامل", true, false),
    BOARD_CHAIR("002", "رئیس هیئت‌مدیره", true, false),
    BOARD_VICE_CHAIR("004", "نایب‌رئیس هیئت‌مدیره", true, false),
    BOARD_MEMBER("006", "عضو هیئت‌مدیره", true, true),
    ALTERNATE_BOARD_MEMBER("007", "عضو علی‌البدل هیئت‌مدیره", true, true),
    MANAGING_PARTNER(null, "شریک مدیر", true, false),
    PARTNER(null, "شریک", true, true),
    SHAREHOLDER("020", "سهام‌دار", true, true),
    LEGAL_REPRESENTATIVE("022", "نماینده قانونی", true, true),
    LEGAL_ENTITY_REPRESENTATIVE(null, "نماینده شخص حقوقی", true, false),
    LIQUIDATOR(null, "مدیر تصفیه", true, true),
    PRIMARY_INSPECTOR("008", "بازرس اصلی", true, true),
    ALTERNATE_INSPECTOR("009", "بازرس علی‌البدل", true, true),
    PARENT_COMPANY(null, "شرکت مادر", false, true),
    SUBSIDIARY(null, "شرکت تابعه", false, true),
    AFFILIATE(null, "شخص حقوقی وابسته", false, true);

    private final String companyRegistrationCode;
    private final String label;
    private final boolean naturalPersonAllowed;
    private final boolean legalPersonAllowed;

    RelatedPersonPostCode(String companyRegistrationCode, String label, boolean naturalPersonAllowed, boolean legalPersonAllowed) {
        this.companyRegistrationCode = companyRegistrationCode;
        this.label = label;
        this.naturalPersonAllowed = naturalPersonAllowed;
        this.legalPersonAllowed = legalPersonAllowed;
    }

    public String label() {
        return label;
    }

    @Override public int id() { return ordinal() + 1; }

    public String companyRegistrationCode() { return companyRegistrationCode; }

    public boolean hasCompanyRegistrationCode() { return companyRegistrationCode != null; }

    public static RelatedPersonPostCode fromCompanyRegistrationCode(String value) {
        for (RelatedPersonPostCode code : values()) {
            if (java.util.Objects.equals(code.companyRegistrationCode, value)) return code;
        }
        throw new IllegalArgumentException("کد سمت ثبت شرکت‌ها ناشناخته است: " + value);
    }

    public boolean supports(RelatedPersonType type) {
        return type == RelatedPersonType.NATURAL ? naturalPersonAllowed : legalPersonAllowed;
    }

    public boolean naturalPersonAllowed() { return naturalPersonAllowed; }
    public boolean legalPersonAllowed() { return legalPersonAllowed; }
}
