package com.mparsict.vosolmaliat.domain.model.masterdata;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tax_unit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tax_affairs_office_id")
    private Long taxAffairsOfficeId;

    @Column(name = "tax_unit_name")
    private String taxUnitName;

    @Column(name = "address")
    private String address;

    @Column(name = "phone")
    private String phone;
}
