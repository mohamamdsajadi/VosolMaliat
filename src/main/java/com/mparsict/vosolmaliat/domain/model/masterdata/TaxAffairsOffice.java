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

    @Column(name = "general_directorate_of_tax_affairs_id")
    private Long generalDirectorateOfTaxAffairsId;

    @Column(name = "office_name")
    private String officeName;
}
