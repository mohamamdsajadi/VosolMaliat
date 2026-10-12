package com.mparsict.vosolmaliat.domain.enums;
public enum RuleScope implements LookupValue {
    ISSUANCE(1, "صدور"), INFORM(2, "ابلاغ");
    private final int id; private final String label;
    RuleScope(int id, String label) { this.id=id; this.label=label; }
    @Override public int id() { return id; }
    @Override public String label() { return label; }
}
