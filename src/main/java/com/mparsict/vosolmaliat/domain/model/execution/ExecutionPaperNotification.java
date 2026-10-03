package com.mparsict.vosolmaliat.domain.model.execution;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "execution_paper_notification")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ExecutionPaperNotification extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "electronic_notification_id", nullable = false, unique = true, updatable = false)
    private String electronicNotificationId;
    @Column(name = "notification_date", nullable = false, updatable = false)
    private LocalDateTime notificationDate;
    @Column(name = "taxpayer_notification_view_date") private LocalDateTime taxpayerNotificationViewDate;
    @Version private Long version;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execution_paper_id", nullable = false, unique = true, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore private ExecutionPaper executionPaper;

    public Long getExecutionPaperId() { return executionPaper == null ? null : executionPaper.getId(); }
    public String getElectronicNotificationDate() { return notificationDate == null ? null : notificationDate.toString(); }
    public void setElectronicNotificationDate(String value) { notificationDate = value == null ? null : LocalDateTime.parse(value); }
    public void setExecutionPaperId(Long id) { executionPaper = id == null ? null : ExecutionPaper.builder().id(id).build(); }
    public LocalDateTime paymentDeadline() { return taxpayerNotificationViewDate == null ? null : taxpayerNotificationViewDate.plusDays(30); }
    public void recordView(LocalDateTime date) {
        if (date == null || notificationDate == null || date.isBefore(notificationDate))
            throw new com.mparsict.vosolmaliat.common.exception.BusinessException("INVALID_VIEW_DATE", "تاریخ رویت نباید خالی یا پیش از تاریخ ابلاغ باشد");
        taxpayerNotificationViewDate = date;
    }
    // API compatibility: processing metadata belongs to the mutable workflow projection.
    private ExecutionWorkflowState workflow() { return executionPaper.getWorkflowState(); }
    public String getInformStatus() { return workflow().getInformStatus(); }
    public String getInformRejectionReason() { return workflow().getInformRejectionReason(); }
    public String getInformRejectedAt() { return workflow().getInformRejectedAt(); }
    public String getReInformDate() { return workflow().getReInformDate(); }
    public String getNextDebtInquiryDate() { return workflow().getNextDebtInquiryDate(); }
}
