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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legal_person_id")
    private LegalPerson legalPerson;

    public Long getLegalPersonId() {
        return legalPerson == null ? null : legalPerson.getId();
    }

    public void setLegalPersonId(Long legalPersonId) {
        this.legalPerson = legalPersonId == null ? null : LegalPerson.builder().id(legalPersonId).build();
    }
}
