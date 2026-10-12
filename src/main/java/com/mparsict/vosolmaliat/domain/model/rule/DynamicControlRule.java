package com.mparsict.vosolmaliat.domain.model.rule;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import com.mparsict.vosolmaliat.domain.enums.RuleScope;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "dynamic_control_rule")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "creation_date", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "update_date", nullable = false))
})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "rule_scope")
@Getter @Setter @SuperBuilder @NoArgsConstructor
public abstract class DynamicControlRule extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version private Long version;
    @Enumerated(EnumType.STRING)
    @Column(name = "rule_scope", insertable = false, updatable = false)
    @Setter(AccessLevel.NONE) private RuleScope ruleScope;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="rule_scope", referencedColumnName="code", insertable=false, updatable=false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private com.mparsict.vosolmaliat.domain.model.masterdata.RuleScopeDefinition ruleScopeDefinition;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "parent_rule_id")
    @com.fasterxml.jackson.annotation.JsonIgnore private DynamicControlRule parentRule;
    @Column(name = "error_code", nullable = false) private String errorCode;
    @Column(name = "error_message", nullable = false) private String errorMessage;
    @Builder.Default @Column(name = "is_active", nullable = false)
    @Setter(AccessLevel.NONE) private Boolean active = true;
    @Column(name = "json", nullable = false, columnDefinition = "TEXT") private String ruleJson;
    @Column(name = "limitation_start_date", nullable = false) private String startDate;
    @Column(name = "limitation_end_date", nullable = false) private String endDate;
    @Column(name = "document_no", nullable = false) private String documentNo;
    @Column(name = "document_file_path", nullable = false) private String filePath;
    @Column(columnDefinition = "TEXT") private String description;
    @OneToMany(mappedBy = "rule", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @Builder.Default @com.fasterxml.jackson.annotation.JsonIgnore
    private List<DynamicControlRuleHistory> history = new ArrayList<>();

    public RuleScope getRuleScope() {
        return this instanceof ExecutionPaperInformRejectionRule ? RuleScope.INFORM : RuleScope.ISSUANCE;
    }

    public void update(String json, String start, String end, String document, String path, String details) {
        if (!Boolean.TRUE.equals(active)) throw new IllegalStateException("قاعده غیرفعال قابل ویرایش نیست.");
        snapshot();
        ruleJson = json; startDate = start; endDate = end; documentNo = document;
        filePath = path; description = details;
    }

    public void deactivate() {
        if (Boolean.FALSE.equals(active)) return;
        snapshot();
        active = false;
    }

    private void snapshot() {
        if (id == null) return;
        LocalDateTime now = LocalDateTime.now();
        for (var row : history) if (row.getNextVersionCreationDate() == null) row.close(now);
        // Store the prior effective version before mutating the current row.
        if (history.isEmpty()) history.add(DynamicControlRuleHistory.from(this, now));
    }

    @PostPersist
    private void establishScope() { ruleScope = getRuleScope(); }

    @PrePersist @PreUpdate
    private void validate() {
        if (parentRule != null && parentRule.getRuleScope() != getRuleScope())
            throw new IllegalStateException("قاعده والد باید در همان حوزه باشد.");
        if (startDate != null && endDate != null && startDate.compareTo(endDate) > 0)
            throw new IllegalStateException("تاریخ پایان قاعده نباید پیش از تاریخ شروع باشد.");
    }

    /** Called by the application service after all fields for this revision are assigned. */
    public void recordVersion() {
        LocalDateTime now = LocalDateTime.now();
        for (var row : history) if (row.getNextVersionCreationDate() == null) row.close(now);
        history.add(DynamicControlRuleHistory.from(this, null));
    }
}
