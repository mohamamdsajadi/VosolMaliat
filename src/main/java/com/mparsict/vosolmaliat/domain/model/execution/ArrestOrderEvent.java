package com.mparsict.vosolmaliat.domain.model.execution;

import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity @Table(name = "arrest_order_event", indexes = @Index(name = "ix_arrest_event_order", columnList = "arrest_order_id,id"))
@Getter @Setter @NoArgsConstructor
public class ArrestOrderEvent extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "arrest_order_id", nullable = false, updatable = false) private ArrestOrder arrestOrder;
    @Column(nullable = false, updatable = false) private String action;
    @Column(nullable = false, updatable = false) private String actorName;
    @Column(nullable = false, updatable = false) private String actorUsername;
    @Column(nullable = false, updatable = false) private String destination;
    @Column(nullable = false, updatable = false) private Instant occurredAt;
    @Column(length = 1000, updatable = false) private String note;
}
