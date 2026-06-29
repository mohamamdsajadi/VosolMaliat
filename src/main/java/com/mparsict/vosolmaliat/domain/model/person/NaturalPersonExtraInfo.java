package com.mparsict.vosolmaliat.domain.model.person;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "natural_person_extra_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NaturalPersonExtraInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "natural_person_id")
    private Long naturalPersonId;

    @Column(name = "arrest_status")
    private Integer arrestStatus;

    @Column(name = "capital_status")
    private Integer capitalStatus;

    @Column(name = "certificate_of_no_criminal_record")
    private Integer certificateOfNoCriminalRecord;

    @Column(name = "is_foreign")
    private Integer isForeign;

    @Column(name = "first_name_en")
    private String firstNameEn;

    @Column(name = "last_name_en")
    private String lastNameEn;

    @Column(name = "management_declaration")
    private Integer managementDeclaration;

    @Column(name = "passport_expiry_date")
    private String passportExpiryDate;

    @Column(name = "created_at")
    private String createdAt;

    @Column(name = "updated_at")
    private String updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now().toString();
        this.updatedAt = LocalDateTime.now().toString();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now().toString();
    }
}
