package com.mparsict.vosolmaliat.domain.model.person;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "natural_person_extra_info")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "creation_date", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "update_date", nullable = false))
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NaturalPersonExtraInfo extends BaseEntity {
    @Version private Long version;
    @Column(name="first_name") private String firstName;
    @Column(name="last_name") private String lastName;
    @Column(name = "full_name", nullable=false) private String fullName;
    @Column(name="father_name") private String fatherName;
    @Column(name="birth_date") private java.time.LocalDateTime birthDate;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "is_foreign", nullable=false)
    private Boolean isForeign;

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
