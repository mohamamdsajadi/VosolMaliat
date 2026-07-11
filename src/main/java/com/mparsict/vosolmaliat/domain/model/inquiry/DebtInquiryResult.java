package com.mparsict.vosolmaliat.domain.model.inquiry;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "debt_inquiry_result")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebtInquiryResult extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "finalized_debt_paper_notification_id")
    private String finalizedDebtPaperNotificationId;

    @Column(name = "response_json", columnDefinition = "TEXT")
    private String responseJson;

    @Column(name = "hash")
    private String hash;
}
