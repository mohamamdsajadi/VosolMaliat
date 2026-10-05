package com.mparsict.vosolmaliat.domain.model.masterdata;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "general_directorate_of_tax_affairs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeneralDirectorateOfTaxAffairs extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "general_directorate_of_tax_affairs_name", nullable=false)
    private String generalDirectorateName;

    @Column(name = "zipcode", nullable=false)
    private String postalCode;

    @Column(name = "address", nullable=false)
    private String address;

    @Column(name = "phone", nullable=false)
    private String phone;
}
