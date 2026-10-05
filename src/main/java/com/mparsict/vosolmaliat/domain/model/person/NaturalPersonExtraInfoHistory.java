package com.mparsict.vosolmaliat.domain.model.person;
import jakarta.persistence.*;
import lombok.*;
import com.mparsict.vosolmaliat.domain.model.BaseEntity;
@Entity @Table(name="natural_person_extra_info_history") @org.hibernate.annotations.Immutable
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "creation_date", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "update_date", nullable = false))
})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class NaturalPersonExtraInfoHistory extends com.mparsict.vosolmaliat.domain.model.VersionHistory {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="natural_person_id", nullable=false) private NaturalPerson naturalPerson;
    @Column(name="first_name") private String firstName;
    @Column(name="last_name") private String lastName;
    @Column(name="full_name") private String fullName;
    @Column(name="father_name") private String fatherName;
    @Column(name="birth_date") private java.time.LocalDateTime birthDate;
    @Column(name="is_foreign") private Boolean isForeign;

    public static NaturalPersonExtraInfoHistory from(NaturalPerson person, NaturalPersonExtraInfo info) {
        var row = NaturalPersonExtraInfoHistory.builder()
                .naturalPerson(person).firstName(info.getFirstName()).lastName(info.getLastName())
                .fullName(info.getFullName()).fatherName(info.getFatherName()).birthDate(info.getBirthDate())
                .isForeign(info.getIsForeign()).build();
        row.captureAudit(info);
        return row;
    }
}
