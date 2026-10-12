package com.mparsict.vosolmaliat.domain.model.masterdata;

import com.mparsict.vosolmaliat.domain.enums.TaxSourceType;
import jakarta.persistence.*;
import lombok.Getter;

@Entity @Table(name="tax_source")
@Getter
public class TaxSourceDefinition {
    @Id private Integer id;
    @Column(name = "name", nullable = false) private String name;

    protected TaxSourceDefinition() {}
    public TaxSourceDefinition(TaxSourceType value) {
        this.id = value.id();
        this.name = value.label();
    }
}
