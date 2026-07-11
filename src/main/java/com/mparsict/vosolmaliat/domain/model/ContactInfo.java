package com.mparsict.vosolmaliat.domain.model;

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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "natural_person_id")
    private Long naturalPersonId;

    @Column(name = "legal_person_id")
    private Long legalPersonId;

    @Column(name = "phone_no")
    private String phoneNo;

    @Column(name = "mobile_no")
    private String mobileNo;

    @Column(name = "fax_no")
    private String faxNo;

    @Column(name = "zip_code")
    private String zipCode;

    @Column(name = "legal_address")
    private String legalAddress;

    @Column(name = "email")
    private String email;

    @Column(name = "website")
    private String website;
}
