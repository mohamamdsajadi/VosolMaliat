package com.mparsict.vosolmaliat.domain.model.integration;

import com.mparsict.vosolmaliat.domain.enums.DebtPaperProcessingStatus;
import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaper;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "debt_paper_push_record", indexes = {
        @Index(name = "uk_debt_paper_push_reference", columnList = "reference_id", unique = true),
        @Index(name = "idx_debt_paper_push_execution", columnList = "execution_paper_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebtPaperPushRecord extends BaseEntity {
    private Long issuanceRequestId;
    private Long processInstanceKey;

    @Column(name = "announcement_type")
    private String announcementType;

    @Column(name = "source_tax_office_code", columnDefinition = "TEXT", updatable = false)
    private String sourceTaxOfficeCode;

    private String mappingVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "final_tax_debt_paper_id")
    private com.mparsict.vosolmaliat.domain.model.execution.FinalTaxDebtPaper finalTaxDebtPaper;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reference_id", nullable = false, unique = true, updatable = false)
    private String referenceId;

    @Column(name = "trace_id", nullable = false, updatable = false)
    private String traceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false)
    private DebtPaperProcessingStatus processingStatus;

    @Column(name = "received_at", nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    @Column(name = "source_produce_date", nullable = false, updatable = false)
    private OffsetDateTime sourceProduceDate;

    @Column(name = "source_hash", updatable = false)
    private String sourceHash;

    @Column(name = "national_id", nullable = false, updatable = false)
    private String nationalId;

    @Column(name = "follow_code", updatable = false)
    private String followCode;

    @Column(name = "second_parameter", updatable = false)
    private String secondParameter;

    @Column(name = "source_year", nullable = false, updatable = false)
    private Integer year;

    @Column(name = "source_period", nullable = false, updatable = false)
    private Integer period;

    @Column(name = "tax_amount", updatable = false)
    private String taxAmount;

    @Column(name = "tax_source", nullable = false, updatable = false)
    private String taxSource;

    @Column(name = "xml_payload", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String xmlPayload;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_paper_id")
    private ExecutionPaper executionPaper;

    public Long getExecutionPaperId() {
        return executionPaper == null ? null : executionPaper.getId();
    }
}
