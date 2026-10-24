package com.mparsict.vosolmaliat.domain.model.execution;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Personal viewing metadata, independent of taxpayer notification and workflow state. */
@Entity
@Table(name = "execution_paper_view")
@IdClass(ExecutionPaperView.Key.class)
@Getter @NoArgsConstructor
public class ExecutionPaperView {
    @Id @Column(name = "user_key", nullable = false, updatable = false)
    private String userKey;
    @Id @Column(name = "execution_paper_id", nullable = false, updatable = false)
    private Long executionPaperId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execution_paper_id", nullable = false, insertable = false, updatable = false)
    private ExecutionPaper executionPaper;
    @Column(name = "last_viewed_at_millis", nullable = false)
    private long lastViewedAtMillis;

    @lombok.Data @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class Key implements java.io.Serializable {
        private String userKey;
        private Long executionPaperId;
    }
}
