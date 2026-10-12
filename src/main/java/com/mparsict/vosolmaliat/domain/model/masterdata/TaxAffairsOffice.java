package com.mparsict.vosolmaliat.domain.model.masterdata;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tax_affairs_office")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxAffairsOffice extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "office_name", nullable=false)
    private String officeName;

    @Column(name = "zipcode", nullable=false)
    private String postalCode;

    @Column(name = "address", nullable=false)
    private String address;

    @Column(name = "phone", nullable=false)
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "general_directorate_of_tax_affairs_id", nullable=false)
    private GeneralDirectorateOfTaxAffairs generalDirectorateOfTaxAffairs;

    public Long getGeneralDirectorateOfTaxAffairsId() {
        return generalDirectorateOfTaxAffairs == null ? null : generalDirectorateOfTaxAffairs.getId();
    }

    public void setGeneralDirectorateOfTaxAffairsId(Long generalDirectorateOfTaxAffairsId) {
        this.generalDirectorateOfTaxAffairs = generalDirectorateOfTaxAffairsId == null
                ? null
                : GeneralDirectorateOfTaxAffairs.builder().id(generalDirectorateOfTaxAffairsId).build();
    }
}
