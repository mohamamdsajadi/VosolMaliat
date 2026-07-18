package com.mparsict.vosolmaliat.domain.model.integration;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaper;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fake_sanim_inform_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FakeSanimInformRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_paper_id")
    private ExecutionPaper executionPaper;

    @Column(name = "message_type")
    private String messageType;

    @Column(name = "debt_paper_id")
    private String debtPaperId;

    @Column(name = "status")
    private String status;

    @Column(name = "inform_date")
    private String informDate;

    @Column(name = "rejection_reason_code")
    private String rejectionReasonCode;

    @Column(name = "rejection_reason_description", columnDefinition = "TEXT")
    private String rejectionReasonDescription;

    @Column(name = "restriction_removal_date")
    private String restrictionRemovalDate;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "sent_at")
    private String sentAt;

    @Column(name = "confirmed_at")
    private String confirmedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public Long getExecutionPaperId() {
        return executionPaper == null ? null : executionPaper.getId();
    }

    public void setExecutionPaperId(Long executionPaperId) {
        this.executionPaper = executionPaperId == null ? null : ExecutionPaper.builder().id(executionPaperId).build();
    }
}
