package com.mparsict.vosolmaliat.domain.model.execution;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import com.mparsict.vosolmaliat.domain.enums.RelatedPersonType;
import com.mparsict.vosolmaliat.domain.model.person.LegalPerson;
import com.mparsict.vosolmaliat.domain.model.person.NaturalPerson;
import com.mparsict.vosolmaliat.domain.model.Taxpayer;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "related_persons")
@AttributeOverride(name = "createdAt", column = @Column(name = "creation_date", nullable = false, updatable = false))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RelatedPerson extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "taxpayer_id", nullable = false)
    private Taxpayer taxpayer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_legal_person_id")
    private LegalPerson ownerLegalPerson;

    // Kept nullable in the Hibernate mapping so ddl-auto=update can add post_id
    // to an existing non-empty SQLite table. The entity callback below and the
    // production migration both enforce that new normalized rows have a post.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private RelatedPersonPost post;

    @Column(name="post_name", nullable=false) private String postName;

    @Column(name = "start_date", nullable=false)
    private String startDate;

    @Column(name = "end_date", nullable=false)
    private String endDate;

    @Column(name = "position_expiry_date", nullable=false)
    private String positionExpiryDate;

    @Column(name = "is_board_member", nullable = false)
    private Boolean boardMember;

    @Column(name = "is_partner", nullable = false)
    private Boolean partner;

    @Column(name = "has_signatory_authority", nullable = false)
    private Boolean hasSignatoryAuthority;

    @Column(name = "management_declaration")
    private Integer managementDeclaration;

    @Column(name = "source_system")
    private String sourceSystem;

    @Column(name = "source_record_id")
    private String sourceRecordId;

    @Column(name = "last_synced_at")
    private String lastSyncedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_natural_person_id")
    private NaturalPerson naturalPerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_legal_person_id")
    private LegalPerson legalPerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "natural_person_related_to_legal_person_id")
    private NaturalPerson legalRepresentativeNaturalPerson;

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
        if (taxpayer == null) {
            throw new IllegalStateException("Related person must belong to a taxpayer");
        }
        if ((naturalPerson == null) == (legalPerson == null)) {
            throw new IllegalStateException("Related person must reference exactly one natural or legal person");
        }
        if (legalPerson != null && legalRepresentativeNaturalPerson == null) {
            throw new IllegalStateException("A related legal person must have a natural-person representative");
        }
        if (legalPerson == null && legalRepresentativeNaturalPerson != null) {
            throw new IllegalStateException("A natural related person cannot have a legal-person representative");
        }
        if (post != null && post.getCode() != null) postName = post.getCode().name();
        RelatedPersonType personType = naturalPerson != null
                ? RelatedPersonType.NATURAL
                : RelatedPersonType.LEGAL;
        if (post == null || post.getCode() == null || !post.getCode().supports(personType)) {
            throw new IllegalStateException("Related person post is not allowed for target person type");
        }
    }
}
