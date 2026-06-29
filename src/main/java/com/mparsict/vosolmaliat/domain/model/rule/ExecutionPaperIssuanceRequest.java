package com.mparsict.vosolmaliat.domain.model.rule;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "execution_paper_issuance_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionPaperIssuanceRequest {

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

    @Column(name = "created_at")
    private String createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now().toString();
    }
}
