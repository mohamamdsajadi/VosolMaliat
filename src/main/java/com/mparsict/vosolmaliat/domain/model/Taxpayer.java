package com.mparsict.vosolmaliat.domain.model;

import com.mparsict.vosolmaliat.domain.enums.TaxpayerType;
import com.mparsict.vosolmaliat.domain.model.person.LegalPerson;
import com.mparsict.vosolmaliat.domain.model.person.NaturalPerson;
import jakarta.persistence.*;
import lombok.*;

@org.hibernate.annotations.Immutable
@jakarta.persistence.EntityListeners(com.mparsict.vosolmaliat.domain.model.ImmutableEntityListener.class)
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
    public void updateActivity(com.mparsict.vosolmaliat.domain.model.masterdata.ActivityType value) {
        if (activityType == null) activityType=TaxpayerActivityType.builder().taxpayer(this).build();
        activityType.changeActivity(value);
    }
    @Column(name="tin", nullable=false, unique=true, updatable=false) private String tin;
    @Column(name="registration_trace_code", nullable=false, unique=true, updatable=false) private String registrationTraceCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "taxpayer_type", nullable = false, updatable = false)
    private TaxpayerType taxpayerType;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "natural_person_id", unique = true)
    private NaturalPerson naturalPerson;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legal_person_id", unique = true)
    private LegalPerson legalPerson;

    public TaxpayerType getType() {
        return taxpayerType != null ? taxpayerType
                : (naturalPerson != null ? TaxpayerType.NATURAL : TaxpayerType.LEGAL);
    }

    @PrePersist
    @PreUpdate
    private void validateIdentity() {
        if ((naturalPerson == null) == (legalPerson == null)) {
            throw new IllegalStateException("Taxpayer must reference exactly one natural or legal person");
        }
        TaxpayerType derived = naturalPerson != null ? TaxpayerType.NATURAL : TaxpayerType.LEGAL;
        if (taxpayerType == null) taxpayerType = derived;
        if (taxpayerType != derived) {
            throw new IllegalStateException("Taxpayer type conflicts with its person reference");
        }
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
