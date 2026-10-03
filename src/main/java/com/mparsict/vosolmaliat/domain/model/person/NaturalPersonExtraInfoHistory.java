package com.mparsict.vosolmaliat.domain.model.person;
import jakarta.persistence.*;
import lombok.*;
import com.mparsict.vosolmaliat.domain.model.BaseEntity;
@Entity @Table(name="natural_person_extra_info_history") @org.hibernate.annotations.Immutable
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class NaturalPersonExtraInfoHistory extends com.mparsict.vosolmaliat.domain.model.VersionHistory {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(nullable=false) private NaturalPerson naturalPerson;
    private String firstName;
    private String lastName;
    private String fullName;
    private String fatherName;
    private java.time.LocalDateTime birthDate;
    private Integer arrestStatus;
    private Integer capitalStatus;
    private Integer certificateOfNoCriminalRecord;
    private Boolean isForeign;
    private String firstNameEn;
    private String lastNameEn;
    private Integer managementDeclaration;
    private String passportExpiryDate;

    public static NaturalPersonExtraInfoHistory from(NaturalPerson person, NaturalPersonExtraInfo info) {
        var row = NaturalPersonExtraInfoHistory.builder()
                .naturalPerson(person).firstName(info.getFirstName()).lastName(info.getLastName())
                .fullName(info.getFullName()).fatherName(info.getFatherName()).birthDate(info.getBirthDate())
                .arrestStatus(info.getArrestStatus()).capitalStatus(info.getCapitalStatus())
                .certificateOfNoCriminalRecord(info.getCertificateOfNoCriminalRecord())
                .isForeign(info.getIsForeign()).firstNameEn(info.getFirstNameEn()).lastNameEn(info.getLastNameEn())
                .managementDeclaration(info.getManagementDeclaration())
                .passportExpiryDate(info.getPassportExpiryDate()).build();
        row.captureAudit(info);
        return row;
    }
}
