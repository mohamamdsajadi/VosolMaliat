package com.mparsict.vosolmaliat.domain.model.masterdata;

import com.mparsict.vosolmaliat.domain.enums.RestrictionState;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity @Table(name="restriction_state")
public class RestrictionStateDefinition extends LookupDefinition {
    protected RestrictionStateDefinition() {}
    public RestrictionStateDefinition(RestrictionState value) { super(value); }
}
