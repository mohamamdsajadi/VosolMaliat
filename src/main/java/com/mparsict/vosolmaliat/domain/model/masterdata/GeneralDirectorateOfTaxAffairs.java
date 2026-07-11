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

    @Column(name = "general_directorate_name")
    private String generalDirectorateName;
}
