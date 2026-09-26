package com.mparsict.vosolmaliat.domain.model.execution;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import com.mparsict.vosolmaliat.domain.model.Taxpayer;
import com.mparsict.vosolmaliat.domain.model.masterdata.TaxUnit;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "execution_paper")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionPaper extends BaseEntity {
    @Column(name="issuance_date", updatable=false) private java.time.LocalDateTime issuanceDate;
    @Column(name="issuance_finalization_reason", updatable=false) private String finalizationReason;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="sending_unit_id", updatable=false) private TaxUnit sendingUnit;

    @Id
    private Long id;

    @Column(name = "execution_paper_unique_code", unique = true)
    private String executionPaperUniqueCode;

    @Column(name = "tax_source")
    private String taxSource;

    // Stores ExecutionPaperState.name()
    @Column(name = "current_state")
    private String currentState;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finalized_debt_paper_id")
    private FinalizedDebtPaper finalizedDebtPaper;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taxpayer_id")
    private Taxpayer taxpayer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_unit_id")
    private TaxUnit taxUnit;

    public Long getFinalizedDebtPaperId() {
        return finalizedDebtPaper == null ? null : finalizedDebtPaper.getId();
    }

    public void setFinalizedDebtPaperId(Long finalizedDebtPaperId) {
        this.finalizedDebtPaper = finalizedDebtPaperId == null ? null : FinalizedDebtPaper.builder().id(finalizedDebtPaperId).build();
    }

    public Long getTaxpayerId() {
        return taxpayer == null ? null : taxpayer.getId();
    }

    public void setTaxpayerId(Long taxpayerId) {
        this.taxpayer = taxpayerId == null ? null : Taxpayer.builder().id(taxpayerId).build();
    }

    public Long getTaxUnitId() {
        return taxUnit == null ? null : taxUnit.getId();
    }

    public void setTaxUnitId(Long taxUnitId) {
        this.taxUnit = taxUnitId == null ? null : TaxUnit.builder().id(taxUnitId).build();
    }
}
