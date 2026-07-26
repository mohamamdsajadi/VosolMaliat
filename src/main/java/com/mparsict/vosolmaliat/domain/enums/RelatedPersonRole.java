package com.mparsict.vosolmaliat.domain.enums;

public enum RelatedPersonRole {
    CEO("مدیرعامل", true, false),
    BOARD_CHAIR("رئیس هیئت‌مدیره", true, false),
    BOARD_VICE_CHAIR("نایب‌رئیس هیئت‌مدیره", true, false),
    BOARD_MEMBER("عضو هیئت‌مدیره", true, true),
    ALTERNATE_BOARD_MEMBER("عضو علی‌البدل هیئت‌مدیره", true, true),
    MANAGING_PARTNER("شریک مدیر", true, false),
    PARTNER("شریک", true, true),
    SHAREHOLDER("سهام‌دار", true, true),
    LEGAL_REPRESENTATIVE("نماینده قانونی", true, true),
    LEGAL_ENTITY_REPRESENTATIVE("نماینده شخص حقوقی", true, false),
    LIQUIDATOR("مدیر تصفیه", true, true),
    PRIMARY_INSPECTOR("بازرس اصلی", true, true),
    ALTERNATE_INSPECTOR("بازرس علی‌البدل", true, true),
    PARENT_COMPANY("شرکت مادر", false, true),
    SUBSIDIARY("شرکت تابعه", false, true),
    AFFILIATE("شخص حقوقی وابسته", false, true);

    private final String label;
    private final boolean naturalPersonAllowed;
    private final boolean legalPersonAllowed;

    RelatedPersonRole(String label, boolean naturalPersonAllowed, boolean legalPersonAllowed) {
        this.label = label;
        this.naturalPersonAllowed = naturalPersonAllowed;
        this.legalPersonAllowed = legalPersonAllowed;
    }

    public String label() {
        return label;
    }

    public boolean supports(RelatedPersonType type) {
        return type == RelatedPersonType.NATURAL ? naturalPersonAllowed : legalPersonAllowed;
    }
}
