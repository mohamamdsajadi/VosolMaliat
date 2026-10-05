package com.mparsict.vosolmaliat.domain.model.execution;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import com.mparsict.vosolmaliat.domain.model.sanim.*;

@Entity @Table(name = "debt_information_history")
@AttributeOverride(name = "createdAt", column = @Column(name = "creation_date", nullable = false, updatable = false))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@EntityListeners(com.mparsict.vosolmaliat.domain.model.ImmutableEntityListener.class)
public class DebtInformationHistory extends BaseEntity {
    @Convert(converter=com.mparsict.vosolmaliat.domain.model.sanim.ExactDecimalConverter.class)
    @Column(name="total_liability", nullable=false, columnDefinition="TEXT")
    private java.math.BigDecimal totalLiability;

    @PrePersist @PreUpdate
    private void updateLiability() {
        var value = amounts;
        if (value != null && value.getTotalTaxAmount() != null && value.getTotalDutyAmount() != null)
            totalLiability = value.getTotalTaxAmount().add(value.getTotalDutyAmount());
    }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "debt_information_id", nullable = false, updatable = false)
    private DebtInformation debtInformation;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "debt_paper_unique_id", nullable = false, updatable = false)
    private FinalTaxDebtPaper finalTaxDebtPaper;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "execution_paper_id", nullable = false, updatable = false)
    private ExecutionPaper executionPaper;
    @Embedded
    @AttributeOverrides({@AttributeOverride(name="totalTaxAmount", column=@Column(name="tax_component", nullable=false, columnDefinition="TEXT", updatable=false)),
        @AttributeOverride(name="totalDutyAmount", column=@Column(name="duty_component", nullable=false, columnDefinition="TEXT", updatable=false)),
        @AttributeOverride(name="totalPenaltyAmount", column=@Column(name="total_penalties", nullable=false, columnDefinition="TEXT", updatable=false)),
        @AttributeOverride(name="totalPaidAmount", column=@Column(name="total_payments", nullable=false, columnDefinition="TEXT", updatable=false)),
        @AttributeOverride(name="totalCreditAmount", column=@Column(name="total_credits", nullable=false, columnDefinition="TEXT", updatable=false)),
        @AttributeOverride(name="totalRemissionAmount", column=@Column(name="total_remissions", nullable=false, columnDefinition="TEXT", updatable=false)),
        @AttributeOverride(name="outstandingDebtAmount", column=@Column(name="outstanding_tax_debt", columnDefinition="TEXT", updatable=false))})
    private com.mparsict.vosolmaliat.domain.model.sanim.SanimAmounts amounts;
    @Embedded private DebtFinancialDetails financialDetails;
    @Column(name = "effective_at", updatable = false) private LocalDateTime effectiveAt;
    @Column(name = "next_inquiry_date") private LocalDateTime nextInquiryDate;
    @Column(name = "inquiry_request_id", updatable = false) private String inquiryRequestId;
    @Version private Long version;

    public static DebtInformationHistory current(DebtInformation debt, String requestId, LocalDateTime receivedAt) {
        var row = DebtInformationHistory.builder().debtInformation(debt)
                .finalTaxDebtPaper(debt.getFinalTaxDebtPaper()).executionPaper(debt.getExecutionPaper())
                .amounts(debt.getCurrentAmounts().copy())
                .financialDetails(debt.getFinancialDetails() == null ? null : debt.getFinancialDetails().copy())
                .effectiveAt(debt.getCurrentAmountsAsOf()).inquiryRequestId(requestId).build();
        row.setCreatedAt(receivedAt);
        return row;
    }
    public void close(LocalDateTime next) {
        if (nextInquiryDate != null) throw new IllegalStateException("نسخه بدهی قبلاً بسته شده است.");
        nextInquiryDate = next;
    }
}
