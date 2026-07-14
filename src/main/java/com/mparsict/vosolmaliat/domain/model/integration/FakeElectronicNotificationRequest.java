package com.mparsict.vosolmaliat.domain.model.integration;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaper;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fake_electronic_notification_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FakeElectronicNotificationRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_paper_id")
    private ExecutionPaper executionPaper;

    @Column(name = "external_tracking_code")
    private String externalTrackingCode;

    @Column(name = "status")
    private String status;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "sent_at")
    private String sentAt;

    @Column(name = "auto_confirm_at")
    private String autoConfirmAt;

    @Column(name = "inform_date")
    private String informDate;

    @Column(name = "confirmed_at")
    private String confirmedAt;

    @Column(name = "callback_delivered_at")
    private String callbackDeliveredAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public Long getExecutionPaperId() {
        return executionPaper == null ? null : executionPaper.getId();
    }

    public void setExecutionPaperId(Long executionPaperId) {
        this.executionPaper = executionPaperId == null ? null : ExecutionPaper.builder().id(executionPaperId).build();
    }
}
