package com.mparsict.vosolmaliat.domain.model.person;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "legal_person_extra_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegalPersonExtraInfo extends BaseEntity {
    @Version private Long version;
    @Column(nullable=false) private String name;
    @Column(name = "trade_name") private String tradeName;
    @Column(name="registration_number", unique=true) private String registrationNumber;
    @Column(name="registration_date") private java.time.LocalDateTime registrationDate;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legal_person_id",unique=true,nullable=false)
    private LegalPerson legalPerson;

    public Long getLegalPersonId() {
        return legalPerson == null ? null : legalPerson.getId();
    }

    public void setLegalPersonId(Long legalPersonId) {
        this.legalPerson = legalPersonId == null ? null : LegalPerson.builder().id(legalPersonId).build();
    }
}
