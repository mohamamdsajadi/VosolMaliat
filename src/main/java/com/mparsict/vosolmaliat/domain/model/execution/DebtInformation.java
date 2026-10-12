package com.mparsict.vosolmaliat.domain.model.execution;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import com.mparsict.vosolmaliat.domain.model.sanim.DebtFinancialDetails;
import com.mparsict.vosolmaliat.domain.model.sanim.ExactDecimalConverter;
import com.mparsict.vosolmaliat.domain.model.sanim.SanimAmounts;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity @Table(name = "debt_information")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DebtInformation extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debt_paper_unique_id", nullable = false, updatable = false)
    private FinalTaxDebtPaper finalTaxDebtPaper;

    @Convert(converter = ExactDecimalConverter.class) @Column(name="total_liability", nullable=false, columnDefinition="TEXT") private BigDecimal totalLiability;
    @Convert(converter = ExactDecimalConverter.class) @Column(name="total_penalties", nullable=false, columnDefinition="TEXT") private BigDecimal totalPenalties;
    @Convert(converter = ExactDecimalConverter.class) @Column(name="total_credits", nullable=false, columnDefinition="TEXT") private BigDecimal totalCredits;
    @Convert(converter = ExactDecimalConverter.class) @Column(name="total_payments", nullable=false, columnDefinition="TEXT") private BigDecimal totalPayments;
    @Convert(converter = ExactDecimalConverter.class) @Column(name="total_remissions", nullable=false, columnDefinition="TEXT") private BigDecimal totalRemissions;
    @Convert(converter = ExactDecimalConverter.class) @Column(name="outstanding_tax_debt", nullable=false, columnDefinition="TEXT") private BigDecimal outstandingTaxDebt;
    @Column(name="penalties_detail", columnDefinition="TEXT") private String penaltiesDetail;
    @Column(name="credits_detail", columnDefinition="TEXT") private String creditsDetail;
    @Column(name="payments_detail", columnDefinition="TEXT") private String paymentsDetail;
    @Column(name="remissions_detail", columnDefinition="TEXT") private String remissionsDetail;

    @Version private Long version;
    @Transient private SanimAmounts currentAmounts;
    @Transient private DebtFinancialDetails financialDetails;

    @PostLoad
    private void loadCompatibilityViews() {
        currentAmounts = SanimAmounts.builder()
                .totalTaxAmount(totalLiability).totalDutyAmount(BigDecimal.ZERO)
                .totalPenaltyAmount(totalPenalties).totalCreditAmount(totalCredits)
                .totalPaidAmount(totalPayments).totalRemissionAmount(totalRemissions)
                .outstandingDebtAmount(outstandingTaxDebt).build();
        financialDetails = DebtFinancialDetails.builder()
                .totalPenaltiesDetails(penaltiesDetail).taxCreditDetails(creditsDetail)
                .totalPaymentsDetails(paymentsDetail).totalRemissionsDetails(remissionsDetail).build();
    }

    @PrePersist @PreUpdate
    private void storeDocumentColumns() {
        if (currentAmounts != null) {
            BigDecimal tax = currentAmounts.getTotalTaxAmount();
            BigDecimal duty = currentAmounts.getTotalDutyAmount();
            totalLiability = tax == null ? duty : duty == null ? tax : tax.add(duty);
            totalPenalties = currentAmounts.getTotalPenaltyAmount();
            totalCredits = currentAmounts.getTotalCreditAmount();
            totalPayments = currentAmounts.getTotalPaidAmount();
            totalRemissions = currentAmounts.getTotalRemissionAmount();
            outstandingTaxDebt = currentAmounts.getOutstandingDebtAmount();
        }
        if (financialDetails != null) {
            penaltiesDetail = financialDetails.getTotalPenaltiesDetails();
            creditsDetail = financialDetails.getTaxCreditDetails();
            paymentsDetail = financialDetails.getTotalPaymentsDetails();
            remissionsDetail = financialDetails.getTotalRemissionsDetails();
        }
    }

    public SanimAmounts getCurrentAmounts() {
        if (currentAmounts == null && totalLiability != null) loadCompatibilityViews();
        return currentAmounts;
    }

    public BigDecimal getExactOutstandingDebt() {
        return currentAmounts == null ? outstandingTaxDebt : currentAmounts.getOutstandingDebtAmount();
    }
}
