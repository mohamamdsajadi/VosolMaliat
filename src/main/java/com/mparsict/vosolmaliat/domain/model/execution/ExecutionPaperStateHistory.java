package com.mparsict.vosolmaliat.domain.model.execution;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@org.hibernate.annotations.Immutable
@EntityListeners(com.mparsict.vosolmaliat.domain.model.ImmutableEntityListener.class)
@Table(name = "execution_paper_state_history")
@AttributeOverride(name = "createdAt", column = @Column(name = "creation_date", nullable = false, updatable = false))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionPaperStateHistory extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="user_id", insertable=false, updatable=false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private com.mparsict.vosolmaliat.domain.model.security.AppUserProfile user;

    @PrePersist
    private void identifyActor() { if (userId == null) userId = com.mparsict.vosolmaliat.domain.model.AuditContext.userId(); }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "state", nullable = false, updatable = false)
    private String state;

    @Column(name = "state_date")
    private String stateDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_paper_id", nullable = false, updatable = false)
    private ExecutionPaper executionPaper;

    public Long getExecutionPaperId() {
        return executionPaper == null ? null : executionPaper.getId();
    }

    public void setExecutionPaperId(Long executionPaperId) {
        this.executionPaper = executionPaperId == null ? null : ExecutionPaper.builder().id(executionPaperId).build();
    }
}
