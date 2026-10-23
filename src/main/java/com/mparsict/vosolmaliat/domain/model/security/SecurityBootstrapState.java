package com.mparsict.vosolmaliat.domain.model.security;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Records one-time defaults so administrator revocations survive restart. */
@Entity
@Table(name = "security_bootstrap_state")
public class SecurityBootstrapState {
    @Id private String name;
    protected SecurityBootstrapState() {}
    public SecurityBootstrapState(String name) { this.name = name; }
}
