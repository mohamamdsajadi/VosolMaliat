package com.mparsict.vosolmaliat.domain.model.execution;

import com.mparsict.vosolmaliat.domain.enums.ArrestOrderStage;
import com.mparsict.vosolmaliat.domain.enums.BusinessRole;
import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import com.mparsict.vosolmaliat.domain.model.security.AppUserProfile;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

/** Durable local process/task projection; the original execution paper stays immutable. */
@Entity @Table(name = "arrest_order", uniqueConstraints = {
        @UniqueConstraint(name = "uk_arrest_order_paper", columnNames = "execution_paper_id")
})
@Getter @Setter @NoArgsConstructor
public class ArrestOrder extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Version private Long version;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "execution_paper_id", nullable = false, updatable = false) private ExecutionPaper executionPaper;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ArrestOrderStage stage;
    @Column(nullable = false, updatable = false) private String engine;
    @Column(nullable = false, updatable = false) private String route;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assignee_id") private AppUserProfile assignee;
    @Enumerated(EnumType.STRING) private BusinessRole candidateRole;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "supervisor_id") private AppUserProfile supervisor;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewer_id") private AppUserProfile reviewer;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "officer_id") private AppUserProfile officer;
    @Column(nullable = false) private Instant referredAt;
    @Column(nullable = false) private String referralDestination;
    @Column(nullable = false, updatable = false) private Instant startedAt;
    private Instant debtInquiredAt;
    private Instant issuedAt;
    @Column(unique = true) private String letterNumber;
    @Column(columnDefinition = "TEXT") private String letterSnapshot;
}
