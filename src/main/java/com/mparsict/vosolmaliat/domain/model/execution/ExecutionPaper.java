package com.mparsict.vosolmaliat.domain.model.execution;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;

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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execution_paper_unique_code", unique = true)
    private String executionPaperUniqueCode;

    @Column(name = "finalized_debt_paper_id")
    private Long finalizedDebtPaperId;

    @Column(name = "taxpayer_id")
    private Long taxpayerId;

    @Column(name = "main_tax_bureau_id")
    private Long mainTaxBureauId;

    @Column(name = "tax_bureau_id")
    private Long taxBureauId;

    @Column(name = "tax_unit_id")
    private Long taxUnitId;

    @Column(name = "sending_unit_id")
    private Long sendingUnitId;

    @Column(name = "finalization_reason")
    private String finalizationReason;

    @Column(name = "tax_source")
    private String taxSource;

    // Stores ExecutionPaperState.name()
    @Column(name = "current_state")
    private String currentState;
}
