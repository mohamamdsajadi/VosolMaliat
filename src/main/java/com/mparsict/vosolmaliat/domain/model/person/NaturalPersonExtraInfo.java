package com.mparsict.vosolmaliat.domain.model.person;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "natural_person_extra_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NaturalPersonExtraInfo extends BaseEntity {
    @Version private Long version;
    private String firstName;
    private String lastName;
    @Column(name = "full_name", nullable=false) private String fullName;
    @Column(nullable=false) private String fatherName;
    @Column(nullable=false) private java.time.LocalDateTime birthDate;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "arrest_status")
    private Integer arrestStatus;

    @Column(name = "capital_status")
    private Integer capitalStatus;

    @Column(name = "certificate_of_no_criminal_record")
    private Integer certificateOfNoCriminalRecord;

    @Column(name = "is_foreign", nullable=false)
    private Boolean isForeign;

    @Column(name = "first_name_en")
    private String firstNameEn;

    @Column(name = "last_name_en")
    private String lastNameEn;

    @Column(name = "management_declaration")
    private Integer managementDeclaration;

    @Column(name = "passport_expiry_date")
    private String passportExpiryDate;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "natural_person_id", unique=true, nullable=false)
    private NaturalPerson naturalPerson;

    public Long getNaturalPersonId() {
        return naturalPerson == null ? null : naturalPerson.getId();
    }

    public void setNaturalPersonId(Long naturalPersonId) {
        this.naturalPerson = naturalPersonId == null ? null : NaturalPerson.builder().id(naturalPersonId).build();
    }
}
