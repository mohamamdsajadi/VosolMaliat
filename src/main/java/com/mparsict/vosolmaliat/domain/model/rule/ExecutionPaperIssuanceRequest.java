package com.mparsict.vosolmaliat.domain.model.rule;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
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

    @Column(name = "debt_paper_unique_code")
    private String debtPaperUniqueCode;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "hash")
    private String hash;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "rejection_reason_id")
    private Long rejectionReasonId;
}
