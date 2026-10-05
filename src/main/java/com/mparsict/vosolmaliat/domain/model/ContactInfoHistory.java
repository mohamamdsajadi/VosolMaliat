package com.mparsict.vosolmaliat.domain.model;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="contact_info_history") @org.hibernate.annotations.Immutable
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ContactInfoHistory extends com.mparsict.vosolmaliat.domain.model.VersionHistory {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="contact_info_id", nullable=false) private ContactInfo contactInfo;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="natural_person_id")
    private com.mparsict.vosolmaliat.domain.model.person.NaturalPerson naturalPerson;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="legal_person_id")
    private com.mparsict.vosolmaliat.domain.model.person.LegalPerson legalPerson;
    @Column(name="phone_no") private String phoneNo;
    @Column(name="mobile_no") private String mobileNo;
    @Column(name="zipcode") private String postalCode;
    @Column(name="address") private String legalAddress;
    private String email;
}
