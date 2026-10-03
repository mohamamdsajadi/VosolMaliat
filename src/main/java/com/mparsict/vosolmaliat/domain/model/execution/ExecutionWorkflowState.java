package com.mparsict.vosolmaliat.domain.model.execution;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/** Operational projection kept separate from the immutable issued document. */
@Entity @Table(name = "execution_workflow_state")
@Getter @Setter @NoArgsConstructor
public class ExecutionWorkflowState extends BaseEntity {
    @Id private Long id;
    @MapsId @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execution_paper_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore private ExecutionPaper executionPaper;
    @Version private Long version;
    @Column(nullable = false) private String currentState;
    private String notificationTrackingId;
    private String informStatus;
    private String informRejectionReason;
    private String informRejectedAt;
    private String reInformDate;
    private String nextDebtInquiryDate;
    @Column(nullable=false) private boolean viewDateMessagePending;
}
