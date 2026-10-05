package com.mparsict.vosolmaliat.domain.model.rule;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import com.mparsict.vosolmaliat.domain.enums.RuleScope;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "dynamic_control_rule_history")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "creation_date", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "update_date", nullable = false))
})
@Getter @NoArgsConstructor
@jakarta.persistence.EntityListeners(com.mparsict.vosolmaliat.domain.model.ImmutableEntityListener.class)
public class DynamicControlRuleHistory extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rule_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore private DynamicControlRule rule;
    @Column(name = "json", nullable = false, updatable = false, columnDefinition = "TEXT") private String ruleJson;
    @Column(name = "limitation_start_date", nullable = false, updatable = false) private String startDate;
    @Column(name = "limitation_end_date", nullable = false, updatable = false) private String endDate;
    @Column(name = "document_no", nullable = false, updatable = false) private String documentNo;
    @Column(name = "document_file_path", nullable = false, updatable = false) private String filePath;
    @Column(updatable = false) private String description;
    @Enumerated(EnumType.STRING) @Column(name = "rule_scope", nullable = false, updatable = false) private RuleScope ruleScope;
    @Column(name = "parent_rule_id", updatable = false) private Long parentRuleId;
    @Column(name = "error_code", nullable = false, updatable = false) private String errorCode;
    @Column(name = "error_message", nullable = false, updatable = false) private String errorMessage;
    @Column(name = "is_active", nullable = false, updatable = false) private Boolean active;
    @Column(name = "history_creation_date", nullable = false, updatable = false) private LocalDateTime effectiveAt;
    @Column(name = "next_version_creation_date") private LocalDateTime nextVersionCreationDate;
    @Version private Long version;

    public void close(LocalDateTime next) {
        if (nextVersionCreationDate != null) throw new IllegalStateException("نسخه تاریخچه قبلاً بسته شده است.");
        nextVersionCreationDate = next;
    }

    public static DynamicControlRuleHistory from(DynamicControlRule source, LocalDateTime next) {
        var row = new DynamicControlRuleHistory();
        row.rule=source; row.ruleJson=source.getRuleJson(); row.startDate=source.getStartDate();
        row.endDate=source.getEndDate(); row.documentNo=source.getDocumentNo(); row.filePath=source.getFilePath();
        row.description=source.getDescription(); row.ruleScope=source.getRuleScope();
        row.parentRuleId=source.getParentRule()==null?null:source.getParentRule().getId();
        row.errorCode=source.getErrorCode(); row.errorMessage=source.getErrorMessage(); row.active=source.getActive();
        row.effectiveAt=LocalDateTime.now(); row.nextVersionCreationDate=next;
        row.setCreatedAt(source.getCreatedAt()); row.setCreatedBy(source.getCreatedBy());
        return row;
    }
}
