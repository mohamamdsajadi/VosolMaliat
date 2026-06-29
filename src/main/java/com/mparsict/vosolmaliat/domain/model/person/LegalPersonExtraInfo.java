package com.mparsict.vosolmaliat.domain.model.person;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "legal_person_extra_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegalPersonExtraInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "legal_person_id")
    private Long legalPersonId;

    @Column(name = "arrest_status")
    private Integer arrestStatus;

    @Column(name = "capital_status")
    private Integer capitalStatus;

    @Column(name = "management_declaration")
    private Integer managementDeclaration;

    @Column(name = "date_of_termination")
    private String dateOfTermination;

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
