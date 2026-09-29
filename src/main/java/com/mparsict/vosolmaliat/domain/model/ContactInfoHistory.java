package com.mparsict.vosolmaliat.domain.model;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="contact_info_history") @org.hibernate.annotations.Immutable
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ContactInfoHistory extends BaseEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(nullable=false) private ContactInfo contactInfo;
    private String phoneNo;
    private String mobileNo;
    private String faxNo;
    private String postalCode;
    private String legalAddress;
    private String email;
    private String website;
}
