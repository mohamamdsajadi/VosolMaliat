package com.mparsict.vosolmaliat.domain.model.execution;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "execution_paper_notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionPaperNotification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "electronic_notification_id")
    private Long electronicNotificationId;

    @Column(name = "electronic_notification_date")
    private String electronicNotificationDate;

    @Column(name = "taxpayer_notification_view_date")
    private String taxpayerNotificationViewDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_paper_id")
    private ExecutionPaper executionPaper;

    public Long getExecutionPaperId() {
        return executionPaper == null ? null : executionPaper.getId();
    }

    public void setExecutionPaperId(Long executionPaperId) {
        this.executionPaper = executionPaperId == null ? null : ExecutionPaper.builder().id(executionPaperId).build();
    }
}
