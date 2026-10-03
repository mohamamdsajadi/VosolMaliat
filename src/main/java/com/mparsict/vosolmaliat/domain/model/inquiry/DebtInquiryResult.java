package com.mparsict.vosolmaliat.domain.model.inquiry;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaper;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Entity
@Immutable
@Table(name = "debt_inquiry_result", indexes = {
        @Index(name = "idx_debt_inquiry_execution_paper", columnList = "execution_paper_id")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebtInquiryResult extends BaseEntity {
    @Column(name="finalized_debt_paper_notification_id", nullable=false, updatable=false)
    private String finalizedDebtPaperNotificationId;

    @Embedded
    private com.mparsict.vosolmaliat.domain.model.sanim.SanimAmounts amounts;

    private LocalDateTime effectiveAt;
    private String mappingVersion;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, unique = true, updatable = false)
    private String requestId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execution_paper_id", nullable = false, updatable = false)
    private ExecutionPaper executionPaper;

    @Column(name = "response_json", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String responseJson;

    @Column(name = "hash", nullable = false, updatable = false)
    private String hash;

    @Column(name = "received_at", nullable = false, updatable = false)
    private LocalDateTime receivedAt;
}
