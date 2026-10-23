package com.mparsict.vosolmaliat.domain.enums;

public enum BusinessRole {
    TAX_COLLECTION_OFFICE("دفتر وصول و اجرای مالیاتی", true),
    SPECIAL_EXECUTION_DEPARTMENT_HEAD("رئیس اداره اجرائیات وصول و اجرای ویژه", true),
    TAX_ORGANIZATION_HEAD("رئیس کل سازمان امور مالیاتی", true),
    DIRECTORATE_EXECUTION_UNIT_HEAD("رئیس واحد اجرائیات اداره کل مربوطه", false),
    DIRECTORATE_EXECUTION_TEAM_SUPERVISOR("سرپرست هیئت اجرایی اداره کل مربوطه", false),
    EXECUTION_OFFICER("مأمور اجرا", false),
    SPECIAL_COLLECTION_DIRECTOR_GENERAL("مدیرکل وصول و اجرای ویژه", true),
    SPECIAL_EXECUTION_DEPUTY("معاون اجرائیات وصول و اجرای ویژه", true),
    EXECUTION_DEPUTY("معاون اجرائیات", false),
    TAX_DIRECTORATE_DIRECTOR_GENERAL("مدیرکل امور مالیاتی اداره کل مربوطه", false),
    ADMIN("مدیر سامانه", true);

    private final String title;
    private final boolean nationalScope;

    BusinessRole(String title, boolean nationalScope) { this.title = title; this.nationalScope = nationalScope; }
    public String getTitle() { return title; }
    public boolean isNationalScope() { return nationalScope; }

    public BusinessRole nextDelegationRole() {
        // The new role catalogue has no approved delegation hierarchy yet.
        return null;
    }

    public boolean isOperational() {
        return this != ADMIN;
    }
}
