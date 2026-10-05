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
    @Column(name = "current_state", nullable = false) private String currentState;
    @Column(name = "notification_tracking_id") private String notificationTrackingId;
    @Column(name = "inform_status") private String informStatus;
    @Column(name = "inform_rejection_reason") private String informRejectionReason;
    @Column(name = "inform_rejected_at") private String informRejectedAt;
    @Column(name = "re_inform_date") private String reInformDate;
    @Column(name = "next_debt_inquiry_date") private String nextDebtInquiryDate;
    @Column(name = "view_date_message_pending", nullable=false) private boolean viewDateMessagePending;
}
