package com.mparsict.vosolmaliat.domain.model.execution;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "debt_information")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebtInformation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_paper_id")
    private ExecutionPaper executionPaper;

    @Builder.Default
    @Column(name = "tax_amount")
    private Double taxAmount = 0.0;

    @Builder.Default
    @Column(name = "duty_amount")
    private Double dutyAmount = 0.0;

    @Builder.Default
    @Column(name = "penalty_article_22")
    private Double penaltyArticle22 = 0.0;

    @Builder.Default
    @Column(name = "penalty_article_23")
    private Double penaltyArticle23 = 0.0;

    @Builder.Default
    @Column(name = "deductible_penalty")
    private Double deductiblePenalty = 0.0;

    @Builder.Default
    @Column(name = "non_deductible_penalty")
    private Double nonDeductiblePenalty = 0.0;

    @Builder.Default
    @Column(name = "tax_paid_amount")
    private Double taxPaidAmount = 0.0;

    @Builder.Default
    @Column(name = "penalty_paid_amount")
    private Double penaltyPaidAmount = 0.0;

    @Builder.Default
    @Column(name = "tax_credit")
    private Double taxCredit = 0.0;

    @Builder.Default
    @Column(name = "remaining_tax")
    private Double remainingTax = 0.0;

    @Builder.Default
    @Column(name = "municipality_share")
    private Double municipalityShare = 0.0;

    @Builder.Default
    @Column(name = "guild_share")
    private Double guildShare = 0.0;

    @Builder.Default
    @Column(name = "compliance_bonus")
    private Double complianceBonus = 0.0;

    @Column(name = "sanim_total_debt_rials")
    private Long sanimTotalDebtRials;

    @Column(name = "sanim_total_penalty_rials")
    private Long sanimTotalPenaltyRials;

    @Column(name = "sanim_total_payment_rials")
    private Long sanimTotalPaymentRials;

    @Column(name = "sanim_total_credit_rials")
    private Long sanimTotalCreditRials;

    @Column(name = "sanim_outstanding_debt_rials")
    private Long sanimOutstandingDebtRials;

    @Column(name = "sanim_last_activity_at")
    private LocalDateTime sanimLastActivityAt;

    @Column(name = "last_debt_inquiry_at")
    private LocalDateTime lastDebtInquiryAt;

    @Transient
    public Double getTotalDebt() {
        double ta = taxAmount != null ? taxAmount : 0.0;
        double da = dutyAmount != null ? dutyAmount : 0.0;
        double p22 = penaltyArticle22 != null ? penaltyArticle22 : 0.0;
        double p23 = penaltyArticle23 != null ? penaltyArticle23 : 0.0;
        double tpa = taxPaidAmount != null ? taxPaidAmount : 0.0;
        double ppa = penaltyPaidAmount != null ? penaltyPaidAmount : 0.0;
        return ta + da + p22 + p23 - tpa - ppa;
    }

    public Long getExecutionPaperId() {
        return executionPaper == null ? null : executionPaper.getId();
    }

    public void setExecutionPaperId(Long executionPaperId) {
        this.executionPaper = executionPaperId == null ? null : ExecutionPaper.builder().id(executionPaperId).build();
    }
}
