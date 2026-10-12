package com.mparsict.vosolmaliat.domain.model.execution;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import com.mparsict.vosolmaliat.domain.model.sanim.ExactDecimalConverter;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "debt_information_history")
@AttributeOverride(name = "createdAt", column = @Column(name = "creation_date", nullable = false, updatable = false))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@EntityListeners(com.mparsict.vosolmaliat.domain.model.ImmutableEntityListener.class)
public class DebtInformationHistory extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "debt_paper_unique_id", nullable = false, updatable = false)
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
    @Column(name="tax_liability_details", columnDefinition="TEXT") private String taxLiabilityDetails;
    @Column(name="outstanding_debt_details", columnDefinition="TEXT") private String outstandingDebtDetails;
    @Column(name = "next_inquiry_date") private LocalDateTime nextInquiryDate;

    public static DebtInformationHistory current(DebtInformation debt, LocalDateTime receivedAt) {
        var amounts = debt.getCurrentAmounts();
        var details = debt.getFinancialDetails();
        var row = DebtInformationHistory.builder().finalTaxDebtPaper(debt.getFinalTaxDebtPaper())
                .totalLiability(amounts == null ? debt.getTotalLiability() : sum(amounts.getTotalTaxAmount(), amounts.getTotalDutyAmount()))
                .totalPenalties(amounts == null ? debt.getTotalPenalties() : amounts.getTotalPenaltyAmount())
                .totalCredits(amounts == null ? debt.getTotalCredits() : amounts.getTotalCreditAmount())
                .totalPayments(amounts == null ? debt.getTotalPayments() : amounts.getTotalPaidAmount())
                .totalRemissions(amounts == null ? debt.getTotalRemissions() : amounts.getTotalRemissionAmount())
                .outstandingTaxDebt(amounts == null ? debt.getOutstandingTaxDebt() : amounts.getOutstandingDebtAmount())
                .taxLiabilityDetails(details == null ? debt.getTaxLiabilityDetails() : details.getTaxLiabilityDetails())
                .penaltiesDetail(details == null ? debt.getPenaltiesDetail() : details.getTotalPenaltiesDetails())
                .creditsDetail(details == null ? debt.getCreditsDetail() : details.getTaxCreditDetails())
                .paymentsDetail(details == null ? debt.getPaymentsDetail() : details.getTotalPaymentsDetails())
                .remissionsDetail(details == null ? debt.getRemissionsDetail() : details.getTotalRemissionsDetails())
                .outstandingDebtDetails(details == null ? debt.getOutstandingDebtDetails() : details.getOutstandingDebtDetails()).build();
        row.setCreatedAt(receivedAt);
        return row;
    }

    private static BigDecimal sum(BigDecimal first, BigDecimal second) {
        return first == null ? second : second == null ? first : first.add(second);
    }
    public void close(LocalDateTime next) {
        if (nextInquiryDate != null) throw new IllegalStateException("نسخه بدهی قبلاً بسته شده است.");
        nextInquiryDate = next;
    }
}
