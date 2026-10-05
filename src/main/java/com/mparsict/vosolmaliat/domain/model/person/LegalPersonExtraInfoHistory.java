package com.mparsict.vosolmaliat.domain.model.person;
import jakarta.persistence.*;
import lombok.*;
import com.mparsict.vosolmaliat.domain.model.BaseEntity;
@Entity @Table(name="legal_person_extra_info_history") @org.hibernate.annotations.Immutable
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class LegalPersonExtraInfoHistory extends com.mparsict.vosolmaliat.domain.model.VersionHistory {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="legal_person_id", nullable=false) private LegalPerson legalPerson;
    private String name;
    @Column(name="trade_name") private String tradeName;
    @Column(name="registration_number") private String registrationNumber;
    @Column(name="registration_date") private java.time.LocalDateTime registrationDate;
    public static LegalPersonExtraInfoHistory from(LegalPerson person, LegalPersonExtraInfo info) {
        var row = LegalPersonExtraInfoHistory.builder()
                .legalPerson(person).name(info.getName()).tradeName(info.getTradeName())
                .registrationNumber(info.getRegistrationNumber()).registrationDate(info.getRegistrationDate()).build();
        row.captureAudit(info);
        return row;
    }
}
