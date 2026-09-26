package com.mparsict.vosolmaliat.domain.model.person;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "legal_person_extra_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegalPersonExtraInfo extends BaseEntity {
    @Version private Long version;
    private String name;
    @Column(unique=true) private String registrationNumber;
    private String registrationDate;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "arrest_status")
    private Integer arrestStatus;

    @Column(name = "capital_status")
    private Integer capitalStatus;

    @Column(name = "management_declaration")
    private Integer managementDeclaration;

    @Column(name = "date_of_termination")
    private String dateOfTermination;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legal_person_id",unique=true,nullable=false)
    private LegalPerson legalPerson;

    public Long getLegalPersonId() {
        return legalPerson == null ? null : legalPerson.getId();
    }

    public void setLegalPersonId(Long legalPersonId) {
        this.legalPerson = legalPersonId == null ? null : LegalPerson.builder().id(legalPersonId).build();
    }
}
