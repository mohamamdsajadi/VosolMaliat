package com.mparsict.vosolmaliat.domain.model.inquiry;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "debt_inquiry_result")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebtInquiryResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "finalized_debt_paper_notification_id")
    private String finalizedDebtPaperNotificationId;

    @Column(name = "response_json", columnDefinition = "TEXT")
    private String responseJson;

    @Column(name = "hash")
    private String hash;

    @Column(name = "created_at")
    private String createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now().toString();
    }
}
