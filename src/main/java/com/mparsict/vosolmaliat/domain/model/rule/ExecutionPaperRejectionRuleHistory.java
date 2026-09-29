package com.mparsict.vosolmaliat.domain.model.rule;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "execution_paper_rejection_rule_history")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ExecutionPaperRejectionRuleHistory extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_id", nullable = false, updatable = false) private ExecutionPaperRejectionRule rule;
    @Column(columnDefinition = "TEXT", updatable = false) private String ruleJson;
    private String startDate;
    private String endDate;
    private String documentNo;
    private String filePath;
    @Column(columnDefinition = "TEXT") private String description;
    private String ruleScope;
    private Long parentRuleId;
    private String errorCode;
    private String errorMessage;
    private Boolean active;
    private String changeType;

    static ExecutionPaperRejectionRuleHistory from(ExecutionPaperRejectionRule source, String type) {
        return ExecutionPaperRejectionRuleHistory.builder().rule(source).ruleJson(source.getRuleJson())
                .startDate(source.getStartDate()).endDate(source.getEndDate()).documentNo(source.getDocumentNo())
                .filePath(source.getFilePath()).description(source.getDescription()).ruleScope(source.getRuleScope())
                .parentRuleId(source.getParentRule() == null ? null : source.getParentRule().getId())
                .errorCode(source.getErrorCode()).errorMessage(source.getErrorMessage())
                .active(source.getActive()).changeType(type).build();
    }
}
