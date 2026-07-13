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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "natural_person_id")
    private NaturalPerson naturalPerson;

    public Long getNaturalPersonId() {
        return naturalPerson == null ? null : naturalPerson.getId();
    }

    public void setNaturalPersonId(Long naturalPersonId) {
        this.naturalPerson = naturalPersonId == null ? null : NaturalPerson.builder().id(naturalPersonId).build();
    }
}
