package com.mparsict.vosolmaliat.domain.model;

import com.mparsict.vosolmaliat.domain.model.person.LegalPerson;
import com.mparsict.vosolmaliat.domain.model.person.NaturalPerson;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "contact_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactInfo extends BaseEntity {
    @Version private Long version;
    @OneToMany(mappedBy="contactInfo", cascade=CascadeType.ALL)
    @Builder.Default private java.util.List<ContactInfoHistory> history = new java.util.ArrayList<>();
    public void snapshot() {
        if (id != null) {
            var row = ContactInfoHistory.builder()
                .contactInfo(this).naturalPerson(naturalPerson).legalPerson(legalPerson).phoneNo(phoneNo).mobileNo(mobileNo)
                .postalCode(postalCode).legalAddress(legalAddress).email(email).build();
            row.captureAudit(this);
            history.add(row);
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_no")
    private String phoneNo;

    @Column(name = "mobile_no")
    private String mobileNo;

    @Column(name = "zipcode", nullable=false)
    private String postalCode;

    @Column(name = "address", nullable=false)
    private String legalAddress;

    @Column(name = "email")
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "natural_person_id")
    private NaturalPerson naturalPerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legal_person_id")
    private LegalPerson legalPerson;

    @PrePersist
    @PreUpdate
    private void validateOwner() {
        if ((naturalPerson == null) == (legalPerson == null)) {
            throw new IllegalStateException("Contact information must belong to exactly one natural or legal person");
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
