package com.mparsict.vosolmaliat.application.dto.response;

import com.mparsict.vosolmaliat.domain.enums.TaxpayerType;
import com.mparsict.vosolmaliat.domain.model.integration.SanimAuditEntry;

import java.time.LocalDateTime;

public record SanimAuditEntryResponse(
        Long id, TaxpayerType taxpayerType, String taxpayerIdentifier, String taxpayerDisplayName,
        SanimAuditEntry.EntryType entryType, Long amountRials, LocalDateTime occurredAt,
        String referenceNo, String description, LocalDateTime recordedAt
) {}
