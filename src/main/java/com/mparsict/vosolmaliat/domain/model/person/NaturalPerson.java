package com.mparsict.vosolmaliat.domain.model.person;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "natural_person")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NaturalPerson extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "national_code", unique = true)
    private String nationalCode;

    @Column(name = "fida", unique = true)
    private String fida;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "father_name", nullable = false)
    private String fatherName;

    @Column(name = "birth_date", nullable = false)
    private String birthDate;
}
