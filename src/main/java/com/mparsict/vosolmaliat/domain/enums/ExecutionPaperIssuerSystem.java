package com.mparsict.vosolmaliat.domain.enums;

public enum ExecutionPaperIssuerSystem {
    SANIM("سنیم"),
    TABRIZ("تبریز");

    private final String label;

    ExecutionPaperIssuerSystem(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
