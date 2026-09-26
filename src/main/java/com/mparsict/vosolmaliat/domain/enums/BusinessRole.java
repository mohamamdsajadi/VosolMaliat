package com.mparsict.vosolmaliat.domain.enums;

public enum BusinessRole {
    GENERAL_OFFICE_HEAD,
    TAX_OFFICE_HEAD,
    TAX_UNIT_HEAD,
    TAX_EXPERT,
    ADMIN;

    public BusinessRole nextDelegationRole() {
        return switch (this) {
            case GENERAL_OFFICE_HEAD -> TAX_OFFICE_HEAD;
            case TAX_OFFICE_HEAD -> TAX_UNIT_HEAD;
            case TAX_UNIT_HEAD -> TAX_EXPERT;
            case TAX_EXPERT, ADMIN -> null;
        };
    }

    public boolean isOperational() {
        return this != ADMIN;
    }
}
