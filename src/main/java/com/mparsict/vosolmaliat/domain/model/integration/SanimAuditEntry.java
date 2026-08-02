package com.mparsict.vosolmaliat.domain.model.integration;

import com.mparsict.vosolmaliat.domain.enums.TaxpayerType;
import com.mparsict.vosolmaliat.domain.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Entity
@Immutable
@Table(name = "sanim_audit_entry", indexes = {
        @Index(name = "idx_sanim_audit_taxpayer", columnList = "taxpayer_type,taxpayer_identifier"),
        @Index(name = "idx_sanim_audit_occurred", columnList = "occurred_at")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SanimAuditEntry extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING) @Column(name = "taxpayer_type", nullable = false, updatable = false)
    private TaxpayerType taxpayerType;
    @Column(name = "taxpayer_identifier", nullable = false, updatable = false)
    private String taxpayerIdentifier;
    @Column(name = "taxpayer_display_name", nullable = false, updatable = false)
    private String taxpayerDisplayName;
    @Enumerated(EnumType.STRING) @Column(name = "entry_type", nullable = false, updatable = false)
    private EntryType entryType;
    @Column(name = "amount_rials", nullable = false, updatable = false)
    private Long amountRials;
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;
    @Column(name = "reference_no", updatable = false)
    private String referenceNo;
    @Column(name = "description", updatable = false)
    private String description;

    public enum EntryType { DEBT, PAYMENT, PENALTY, CREDIT }
}
