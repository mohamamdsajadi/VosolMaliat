package com.mparsict.vosolmaliat.domain.model.masterdata;

import com.mparsict.vosolmaliat.domain.enums.FinalizationReason;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity @Table(name="finalization_reason")
public class FinalizationReasonDefinition extends LookupDefinition {
    protected FinalizationReasonDefinition() {}
    public FinalizationReasonDefinition(FinalizationReason value) { super(value); }
}
