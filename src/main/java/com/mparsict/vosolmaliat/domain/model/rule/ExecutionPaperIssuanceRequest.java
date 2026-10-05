package com.mparsict.vosolmaliat.domain.model.rule;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@org.hibernate.annotations.Immutable
@EntityListeners(com.mparsict.vosolmaliat.domain.model.ImmutableEntityListener.class)
@Table(name = "execution_paper_issuance_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionPaperIssuanceRequest extends BaseEntity {
    @Column(name="xml", columnDefinition="TEXT") private String xml;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "debt_paper_unique_code", nullable=false, updatable=false)
    private String debtPaperUniqueCode;

    @Column(name = "payload", nullable=false, updatable=false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "hash", nullable=false, updatable=false)
    private String hash;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "execution_paper_issuance_request_rejection_reason_id")
    private Long rejectionReasonId;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="execution_paper_issuance_request_rejection_reason_id", insertable=false, updatable=false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private DynamicControlRule rejectionRule;

    @Column(name="original_request_id", updatable=false) private Long originalRequestId;
}
