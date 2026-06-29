package com.mparsict.vosolmaliat.domain.model.execution;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "related_persons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RelatedPerson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execution_paper_id")
    private Long executionPaperId;

    @Column(name = "natural_person_id")
    private Long naturalPersonId;

    @Column(name = "legal_person_id")
    private Long legalPersonId;

    @Column(name = "post_name")
    private String postName;

    @Column(name = "start_date")
    private String startDate;

    @Column(name = "end_date")
    private String endDate;

    @Column(name = "position_expiry_date")
    private String positionExpiryDate;

    @Column(name = "is_non_board_member")
    private Integer isNonBoardMember;

    @Column(name = "is_non_partner")
    private Integer isNonPartner;

    @Column(name = "has_signatory_authority")
    private Integer hasSignatoryAuthority;

    @Column(name = "management_declaration")
    private Integer managementDeclaration;

    @Column(name = "created_at")
    private String createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now().toString();
    }
}
