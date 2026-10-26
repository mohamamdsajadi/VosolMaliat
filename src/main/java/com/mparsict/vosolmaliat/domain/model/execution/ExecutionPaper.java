package com.mparsict.vosolmaliat.domain.model.execution;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import com.mparsict.vosolmaliat.domain.enums.ExecutionPaperIssuerSystem;
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
    @Column(name="old_execution_letter_number", updatable=false) private String oldExecutionLetterNumber;
    @Column(name="old_execution_letter_date", updatable=false) private java.time.LocalDateTime oldExecutionLetterDate;
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execution_paper_unique_code", nullable = false, unique = true, updatable = false)
    private String executionPaperUniqueCode;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.ColumnDefault("'SANIM'")
    @Column(name = "issuer_system", nullable = false, updatable = false, length = 16)
    private ExecutionPaperIssuerSystem issuerSystem = ExecutionPaperIssuerSystem.SANIM;

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
    @JoinColumn(name = "final_tax_debt_paper_id", nullable = false, updatable = false)
    private FinalTaxDebtPaper finalTaxDebtPaper;

    public Long getFinalTaxDebtPaperId() {
        return finalTaxDebtPaper == null ? null : finalTaxDebtPaper.getId();
    }

    public void setFinalTaxDebtPaperId(Long finalTaxDebtPaperId) {
        this.finalTaxDebtPaper = finalTaxDebtPaperId == null ? null : FinalTaxDebtPaper.builder().id(finalTaxDebtPaperId).build();
    }

}
