package com.mparsict.vosolmaliat.domain.model;

import com.mparsict.vosolmaliat.domain.enums.TaxpayerType;
import com.mparsict.vosolmaliat.domain.model.person.LegalPerson;
import com.mparsict.vosolmaliat.domain.model.person.NaturalPerson;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "taxpayer")
@org.hibernate.annotations.Check(constraints="(natural_person_id IS NOT NULL AND legal_person_id IS NULL) OR (natural_person_id IS NULL AND legal_person_id IS NOT NULL)")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Taxpayer extends BaseEntity {
    @OneToOne(mappedBy="taxpayer", cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    private TaxpayerActivityType activityType;
    public void updateActivity(String title) {
        if (title == null || title.isBlank()) return;
        if (activityType == null) activityType=TaxpayerActivityType.builder().taxpayer(this).build();
        activityType.updateTitle(title);
    }
    @Column(name="tin", nullable=false, unique=true, updatable=false) private String tin;
    @Column(name="registration_trace_code", nullable=false, unique=true, updatable=false) private String registrationTraceCode;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "natural_person_id")
    private NaturalPerson naturalPerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legal_person_id", unique = true)
    private LegalPerson legalPerson;

    @Transient
    public TaxpayerType getType() {
        return (naturalPerson != null) ? TaxpayerType.NATURAL : TaxpayerType.LEGAL;
    }

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
