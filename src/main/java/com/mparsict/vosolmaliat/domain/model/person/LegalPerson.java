package com.mparsict.vosolmaliat.domain.model.person;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "legal_person")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegalPerson extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "national_id", unique = true)
    private String nationalId;

    @Column(name = "registration_number", unique = true)
    private String registrationNumber;

    @Column(name = "registration_date", nullable = false)
    private String registrationDate;

    @Column(name = "name", nullable = false)
    private String name;
}
