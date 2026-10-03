package com.mparsict.vosolmaliat.domain.model.execution;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import com.mparsict.vosolmaliat.domain.model.Taxpayer;

import jakarta.persistence.*;
import lombok.*;

@Entity
@org.hibernate.annotations.Immutable
@EntityListeners(com.mparsict.vosolmaliat.domain.model.ImmutableEntityListener.class)
@Table(name = "execution_paper")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionPaper extends BaseEntity {
    @Column(name="issuance_date", nullable=false, updatable=false) private java.time.LocalDateTime issuanceDate;
    @Column(name="issuance_finalization_reason", updatable=false) private String finalizationReason;
    @Column(name="old_execution_letter_number", updatable=false) private String oldExecutionLetterNumber;
    @Column(name="old_execution_letter_date", updatable=false) private java.time.LocalDateTime oldExecutionLetterDate;
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execution_paper_unique_code", nullable = false, unique = true, updatable = false)
    private String executionPaperUniqueCode;

    @Column(name = "tax_source")
    private String taxSource;

    @OneToOne(mappedBy = "executionPaper", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private ExecutionWorkflowState workflowState;

    public String getCurrentState() { return workflowState == null ? null : workflowState.getCurrentState(); }
    public void setCurrentState(String state) {
        com.mparsict.vosolmaliat.domain.enums.ExecutionPaperState.valueOf(state);
        if (workflowState == null) workflowState = new ExecutionWorkflowState();
        workflowState.setExecutionPaper(this);
        workflowState.setCurrentState(state);
    }
    @PrePersist
    private void initializeWorkflow() {
        if (workflowState == null) setCurrentState("UNASSIGNED");
        workflowState.setExecutionPaper(this);
    }
    public static class ExecutionPaperBuilder {
        public ExecutionPaperBuilder currentState(String value) {
            workflowState = new ExecutionWorkflowState();
            workflowState.setCurrentState(value);
            return this;
        }
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finalized_debt_paper_id", nullable = false, updatable = false)
    private FinalizedDebtPaper finalizedDebtPaper;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "taxpayer_id", nullable = false, updatable = false)
    private Taxpayer taxpayer;

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

}
