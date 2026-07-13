package com.mparsict.vosolmaliat.domain.model.inquiry;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import com.mparsict.vosolmaliat.domain.model.person.LegalPerson;
import com.mparsict.vosolmaliat.domain.model.person.NaturalPerson;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "restriction_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestrictionRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "finalized_debt_paper_notification_id")
    private String finalizedDebtPaperNotificationId;

    @Column(name = "result")
    private Integer result;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "natural_person_id")
    private NaturalPerson naturalPerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legal_person_id")
    private LegalPerson legalPerson;

    public Long getNaturalPersonId() {
        return naturalPerson == null ? null : naturalPerson.getId();
    }

    public void setNaturalPersonId(Long naturalPersonId) {
        this.naturalPerson = naturalPersonId == null ? null : NaturalPerson.builder().id(naturalPersonId).build();
    }

    public Long getLegalPersonId() {
        return legalPerson == null ? null : legalPerson.getId();
    }

    public void setLegalPersonId(Long legalPersonId) {
        this.legalPerson = legalPersonId == null ? null : LegalPerson.builder().id(legalPersonId).build();
    }
}
