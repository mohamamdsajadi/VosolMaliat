package com.mparsict.vosolmaliat.application.service;

/** Raw SANIM XML tags used by debt-paper header mappings. */
public enum SanimXmlTag {
    B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V, W, X;

    public String code() {
        return name();
    }
}
