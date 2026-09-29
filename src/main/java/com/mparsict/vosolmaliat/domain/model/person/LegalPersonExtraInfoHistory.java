package com.mparsict.vosolmaliat.domain.model.person;
import jakarta.persistence.*;
import lombok.*;
import com.mparsict.vosolmaliat.domain.model.BaseEntity;
@Entity @Table(name="legal_person_extra_info_history") @org.hibernate.annotations.Immutable
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class LegalPersonExtraInfoHistory extends BaseEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(nullable=false) private LegalPerson legalPerson;
    private String name;
    private String tradeName;
    private String registrationNumber;
    private String registrationDate;
    private Integer arrestStatus;
    private Integer capitalStatus;
    private Integer managementDeclaration;
    private String dateOfTermination;

    public static LegalPersonExtraInfoHistory from(LegalPerson person, LegalPersonExtraInfo info) {
        return LegalPersonExtraInfoHistory.builder()
                .legalPerson(person).name(info.getName()).tradeName(info.getTradeName())
                .registrationNumber(info.getRegistrationNumber()).registrationDate(info.getRegistrationDate())
                .arrestStatus(info.getArrestStatus()).capitalStatus(info.getCapitalStatus())
                .managementDeclaration(info.getManagementDeclaration())
                .dateOfTermination(info.getDateOfTermination()).build();
    }
}
