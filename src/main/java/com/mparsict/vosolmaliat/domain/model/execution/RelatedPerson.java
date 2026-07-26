package com.mparsict.vosolmaliat.domain.model.execution;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import com.mparsict.vosolmaliat.domain.enums.RelatedPersonType;
import com.mparsict.vosolmaliat.domain.model.person.LegalPerson;
import com.mparsict.vosolmaliat.domain.model.person.NaturalPerson;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "related_persons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RelatedPerson extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_legal_person_id")
    private LegalPerson ownerLegalPerson;

    // Kept nullable in the Hibernate mapping so ddl-auto=update can add post_id
    // to an existing non-empty SQLite table. The entity callback below and the
    // production migration both enforce that new normalized rows have a post.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private RelatedPersonPost post;

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

    @Column(name = "source_system")
    private String sourceSystem;

    @Column(name = "source_record_id")
    private String sourceRecordId;

    @Column(name = "last_synced_at")
    private String lastSyncedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "natural_person_id")
    private NaturalPerson naturalPerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legal_person_id")
    private LegalPerson legalPerson;

    public Long getOwnerLegalPersonId() {
        return ownerLegalPerson == null ? null : ownerLegalPerson.getId();
    }

    public void setOwnerLegalPersonId(Long ownerLegalPersonId) {
        this.ownerLegalPerson = ownerLegalPersonId == null
                ? null
                : LegalPerson.builder().id(ownerLegalPersonId).build();
    }

    public Long getNaturalPersonId() {
        return naturalPerson == null ? null : naturalPerson.getId();
    }

    public void setNaturalPersonId(Long naturalPersonId) {
        this.naturalPerson = naturalPersonId == null ? null : NaturalPerson.builder().id(naturalPersonId).build();
    }

    public Long getLegalPersonId() {
        return legalPerson == null ? null : legalPerson.getId();
    }

    public void setLegalPersonId(Long legalPersonId) {
        this.legalPerson = legalPersonId == null ? null : LegalPerson.builder().id(legalPersonId).build();
    }

    @PrePersist
    @PreUpdate
    private void validateRelationship() {
        if (ownerLegalPerson == null) {
            throw new IllegalStateException("Related person must belong to an owner legal person");
        }
        if ((naturalPerson == null) == (legalPerson == null)) {
            throw new IllegalStateException("Related person must reference exactly one natural or legal person");
        }
        RelatedPersonType personType = naturalPerson != null
                ? RelatedPersonType.NATURAL
                : RelatedPersonType.LEGAL;
        if (post == null || post.getCode() == null || !post.getCode().supports(personType)) {
            throw new IllegalStateException("Related person post is not allowed for target person type");
        }
    }
}
