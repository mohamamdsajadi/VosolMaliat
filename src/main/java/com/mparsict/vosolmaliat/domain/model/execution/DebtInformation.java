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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="finalized_debt_paper_id") private FinalizedDebtPaper finalizedDebtPaper;
    @Embedded private com.mparsict.vosolmaliat.domain.model.sanim.DebtFinancialDetails financialDetails;

    @Version
    private Long version;

    @Embedded
    private com.mparsict.vosolmaliat.domain.model.sanim.SanimAmounts currentAmounts;

    private LocalDateTime currentAmountsAsOf;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_paper_id", unique=true)
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
