package com.mparsict.vosolmaliat.application.dto.request;

import com.mparsict.vosolmaliat.domain.model.integration.SanimAuditEntry;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record SanimAuditEntryRequest(
        @NotNull SanimAuditEntry.EntryType entryType,
        @NotNull @Positive Long amountRials,
        LocalDateTime occurredAt,
        String referenceNo,
        String description
) {}
