package com.mparsict.vosolmaliat.domain.model.execution;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "execution_paper_notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionPaperNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execution_paper_id")
    private Long executionPaperId;

    @Column(name = "electronic_notification_id")
    private Long electronicNotificationId;

    @Column(name = "electronic_notification_date")
    private String electronicNotificationDate;

    @Column(name = "taxpayer_notification_view_date")
    private String taxpayerNotificationViewDate;

    @Column(name = "created_at")
    private String createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now().toString();
    }
}
