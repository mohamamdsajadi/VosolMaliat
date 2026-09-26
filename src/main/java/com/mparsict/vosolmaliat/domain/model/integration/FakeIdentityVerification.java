package com.mparsict.vosolmaliat.domain.model.integration;
import jakarta.persistence.*;
import lombok.*;
import com.mparsict.vosolmaliat.domain.model.BaseEntity;
@Entity @Table(name="fake_identity_verification")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FakeIdentityVerification extends BaseEntity {
    @Id private String identifier;
    @Column(nullable=false) private String personType;
    @Column(nullable=false) private String status;
    private String firstName;
    private String lastName;
    private String fatherName;
    private String birthDate;
    private String companyName;
    private String registrationNumber;
    private String registrationDate;
    private String rejectionReason;
}
