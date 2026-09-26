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
    private String registrationNumber;
    private String registrationDate;
}
