package com.mparsict.vosolmaliat.domain.model.masterdata;

import com.mparsict.vosolmaliat.domain.enums.TaxpayerType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity @Table(name="taxpayer_type")
public class TaxpayerTypeDefinition extends LookupDefinition {
    protected TaxpayerTypeDefinition() {}
    public TaxpayerTypeDefinition(TaxpayerType value) { super(value); }
}
