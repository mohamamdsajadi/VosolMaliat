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
    @Convert(converter=com.mparsict.vosolmaliat.domain.model.sanim.ExactDecimalConverter.class)
    @Column(name="total_liability", nullable=false, columnDefinition="TEXT")
    private java.math.BigDecimal totalLiability;

    @PrePersist @PreUpdate
    private void updateLiability() {
        var value = currentAmounts;
        if (value != null && value.getTotalTaxAmount() != null && value.getTotalDutyAmount() != null)
            totalLiability = value.getTotalTaxAmount().add(value.getTotalDutyAmount());
    }
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="debt_paper_unique_id", nullable=false, updatable=false) private FinalTaxDebtPaper finalTaxDebtPaper;
    @Embedded private com.mparsict.vosolmaliat.domain.model.sanim.DebtFinancialDetails financialDetails;

    @Version
    private Long version;

    @Embedded
    @AttributeOverrides({@AttributeOverride(name="totalTaxAmount", column=@Column(name="tax_component", nullable=false, columnDefinition="TEXT")),
        @AttributeOverride(name="totalDutyAmount", column=@Column(name="duty_component", nullable=false, columnDefinition="TEXT")),
        @AttributeOverride(name="totalPenaltyAmount", column=@Column(name="total_penalties", nullable=false, columnDefinition="TEXT")),
        @AttributeOverride(name="totalPaidAmount", column=@Column(name="total_payments", nullable=false, columnDefinition="TEXT")),
        @AttributeOverride(name="totalCreditAmount", column=@Column(name="total_credits", nullable=false, columnDefinition="TEXT")),
        @AttributeOverride(name="totalRemissionAmount", column=@Column(name="total_remissions", nullable=false, columnDefinition="TEXT")),
        @AttributeOverride(name="outstandingDebtAmount", column=@Column(name="outstanding_tax_debt", columnDefinition="TEXT"))})
    private com.mparsict.vosolmaliat.domain.model.sanim.SanimAmounts currentAmounts;

    @Column(name = "current_amounts_as_of")
    private LocalDateTime currentAmountsAsOf;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_paper_id", unique=true, nullable=false, updatable=false)
    private ExecutionPaper executionPaper;

    @Column(name = "last_debt_inquiry_at")
    private LocalDateTime lastDebtInquiryAt;

    @Transient
    public java.math.BigDecimal getExactOutstandingDebt() {
        return currentAmounts == null ? null : currentAmounts.getOutstandingDebtAmount();
    }

    public Long getExecutionPaperId() {
        return executionPaper == null ? null : executionPaper.getId();
    }

    public void setExecutionPaperId(Long executionPaperId) {
        this.executionPaper = executionPaperId == null ? null : ExecutionPaper.builder().id(executionPaperId).build();
    }
}
