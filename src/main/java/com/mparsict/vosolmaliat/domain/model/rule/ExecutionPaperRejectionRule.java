package com.mparsict.vosolmaliat.domain.model.rule;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "execution_paper_rejection_rule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionPaperRejectionRule extends BaseEntity {

    @Version
    private Long version;

    @Builder.Default
    @Column(name = "rule_scope", nullable = false)
    private String ruleScope = "ISSUANCE";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_rule_id")
    private ExecutionPaperRejectionRule parentRule;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "rule", cascade = CascadeType.ALL)
    @Builder.Default
    private java.util.List<ExecutionPaperRejectionRuleHistory> history = new java.util.ArrayList<>();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_json", columnDefinition = "TEXT")
    private String ruleJson;

    @Column(name = "start_date")
    private String startDate;

    @Column(name = "end_date")
    private String endDate;

    @Column(name = "document_no")
    private String documentNo;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    public void update(String json, String start, String end, String document, String path, String details) {
        snapshot("UPDATE");
        ruleJson = json; startDate = start; endDate = end; documentNo = document;
        filePath = path; description = details;
    }

    public void deactivate() {
        if (Boolean.FALSE.equals(active)) return;
        snapshot("DEACTIVATE");
        active = false;
    }

    private void snapshot(String changeType) {
        if (id != null) history.add(ExecutionPaperRejectionRuleHistory.from(this, changeType));
    }
}
