package com.mparsict.vosolmaliat.domain.enums;

public enum RestrictionState implements LookupValue {
    REGISTRATION_PENDING(0, "در حال ارسال ثبت"),
    REGISTERED(1, "ثبت شده"),
    RELEASE_PENDING(2, "در حال ارسال لغو"),
    RELEASED(3, "لغو شده");

    private final int id;
    private final String label;
    RestrictionState(int id, String label) { this.id=id; this.label=label; }
    @Override public int id() { return id; }
    @Override public String label() { return label; }
}
