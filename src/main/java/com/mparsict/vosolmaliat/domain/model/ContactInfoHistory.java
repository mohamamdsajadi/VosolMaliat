package com.mparsict.vosolmaliat.domain.model;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="contact_info_history") @org.hibernate.annotations.Immutable
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ContactInfoHistory extends com.mparsict.vosolmaliat.domain.model.VersionHistory {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(nullable=false) private ContactInfo contactInfo;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="natural_person_id")
    private com.mparsict.vosolmaliat.domain.model.person.NaturalPerson naturalPerson;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="legal_person_id")
    private com.mparsict.vosolmaliat.domain.model.person.LegalPerson legalPerson;
    private String phoneNo;
    private String mobileNo;
    private String faxNo;
    private String postalCode;
    private String legalAddress;
    private String email;
    private String website;
}
