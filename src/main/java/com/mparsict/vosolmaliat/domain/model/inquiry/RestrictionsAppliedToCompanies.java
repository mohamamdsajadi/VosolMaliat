package com.mparsict.vosolmaliat.domain.model.inquiry;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import com.mparsict.vosolmaliat.domain.enums.RestrictionState;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaper;
import com.mparsict.vosolmaliat.domain.model.person.NaturalPerson;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "restrictions_applied_to_companies")
@AttributeOverride(name = "createdAt", column = @Column(name = "creation_date", nullable = false, updatable = false))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestrictionsAppliedToCompanies extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execution_paper_id", nullable = false, updatable = false)
    private ExecutionPaper executionPaper;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "natural_person_id", nullable = false, updatable = false)
    private NaturalPerson naturalPerson;

    @Convert(converter = com.mparsict.vosolmaliat.domain.model.masterdata.EnumLookupConverters.RestrictionStateConverter.class)
    @Column(name="restriction_state_id", nullable=false)
    private RestrictionState restrictionState;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="restriction_state_id", insertable=false, updatable=false)
    private com.mparsict.vosolmaliat.domain.model.masterdata.RestrictionStateDefinition restrictionStateDefinition;

    @Column(name="restriction_date") private java.time.LocalDateTime restrictionDate;
    @Column(name="restriction_tracking_code", unique=true) private String restrictionTrackingCode;
    @Column(name="release_date") private java.time.LocalDateTime releaseDate;
    @Column(name="release_tracking_code", unique=true) private String releaseTrackingCode;

    @PrePersist
    @PreUpdate
    private void validateTarget() {
        if (executionPaper == null || naturalPerson == null || restrictionState == null)
            throw new IllegalStateException("Restriction requires execution paper, natural person and state");
    }

    public Long getNaturalPersonId() {
        return naturalPerson == null ? null : naturalPerson.getId();
    }

    public void setNaturalPersonId(Long naturalPersonId) {
        this.naturalPerson = naturalPersonId == null ? null : NaturalPerson.builder().id(naturalPersonId).build();
    }

    public Long getExecutionPaperId() { return executionPaper == null ? null : executionPaper.getId(); }
    public void setExecutionPaperId(Long id) { executionPaper = id == null ? null : ExecutionPaper.builder().id(id).build(); }
}
