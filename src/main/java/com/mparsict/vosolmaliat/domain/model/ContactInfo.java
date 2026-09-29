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
        if (id != null) history.add(ContactInfoHistory.builder()
                .contactInfo(this).phoneNo(phoneNo).mobileNo(mobileNo).faxNo(faxNo)
                .postalCode(postalCode).legalAddress(legalAddress).email(email).website(website).build());
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone_no")
    private String phoneNo;

    @Column(name = "mobile_no")
    private String mobileNo;

    @Column(name = "fax_no")
    private String faxNo;

    @Column(name = "postal_code")
    private String postalCode;

    @Column(name = "legal_address")
    private String legalAddress;

    @Column(name = "email")
    private String email;

    @Column(name = "website")
    private String website;

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
