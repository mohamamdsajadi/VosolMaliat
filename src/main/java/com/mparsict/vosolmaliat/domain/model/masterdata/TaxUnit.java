package com.mparsict.vosolmaliat.domain.model.masterdata;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tax_unit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxUnit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tax_unit_name")
    private String taxUnitName;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "address")
    private String address;

    @Column(name = "phone")
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_affairs_office_id")
    private TaxAffairsOffice taxAffairsOffice;

    public Long getTaxAffairsOfficeId() {
        return taxAffairsOffice == null ? null : taxAffairsOffice.getId();
    }

    public void setTaxAffairsOfficeId(Long taxAffairsOfficeId) {
        this.taxAffairsOffice = taxAffairsOfficeId == null ? null : TaxAffairsOffice.builder().id(taxAffairsOfficeId).build();
    }
}
