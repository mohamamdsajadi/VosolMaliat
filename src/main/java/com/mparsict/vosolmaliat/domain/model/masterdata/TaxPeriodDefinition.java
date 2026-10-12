package com.mparsict.vosolmaliat.domain.model.masterdata;

import com.mparsict.vosolmaliat.domain.enums.TaxPeriod;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity @Table(name="tax_period")
public class TaxPeriodDefinition extends LookupDefinition {
    protected TaxPeriodDefinition() {}
    public TaxPeriodDefinition(TaxPeriod value) { super(value); }
}
