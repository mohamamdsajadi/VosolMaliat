package com.mparsict.vosolmaliat.domain.model.inquiry;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "debt_inquiry_result")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@EntityListeners(com.mparsict.vosolmaliat.domain.model.ImmutableEntityListener.class)
public class DebtInquiryResult extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="finalized_debt_paper_notification_id", nullable=false, updatable=false)
    private String finalTaxDebtPaperNotificationId;
    @Column(name = "xml", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String responseJson;
    @Column(name = "hash", nullable = false, updatable = false)
    private String hash;
}
