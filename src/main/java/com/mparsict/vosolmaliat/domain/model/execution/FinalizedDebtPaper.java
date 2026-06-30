package com.mparsict.vosolmaliat.domain.model.execution;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "finalized_debt_paper")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinalizedDebtPaper {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "debt_paper_unique_code", unique = true)
    private String debtPaperUniqueCode;

    @Column(name = "debt_paper_issue_date")
    private String debtPaperIssueDate;

    @Column(name = "notification_id")
    private String notificationId;

    @Column(name = "finalization_reason")
    private String finalizationReason;

    @Column(name = "tax_source")
    private String taxSource;

    @Column(name = "performance_year")
    private String performanceYear;

    @Column(name = "performance_period")
    private String performancePeriod;

    @Column(name = "activity_type_code")
    private String activityTypeCode;

    @Column(name = "main_tax_bureau_id")
    private Long mainTaxBureauId;

    @Column(name = "tax_bureau_id")
    private Long taxBureauId;

    @Column(name = "tax_unit_id")
    private Long taxUnitId;

    @Column(name = "total_finalized_amount")
    private Double totalFinalizedAmount;

    @Column(name = "finalized_tax_and_non_deductible_amount")
    private Double finalizedTaxAndNonDeductibleAmount;

    @Column(name = "created_at")
    private String createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now().toString();
    }
}
