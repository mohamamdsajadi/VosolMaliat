package com.mparsict.vosolmaliat.application.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/** Lightweight current-balance projection; never includes historical snapshots. */
public record DashboardPaperRow(
        Long id, String uniqueCode, LocalDateTime issuanceDate, Long taxpayerId,
        String taxpayerCode, String taxpayerName, String nationalId, String state,
        boolean notified, Long directorateId, String directorateName,
        Long officeId, String officeName, Long debtId, BigDecimal liability, BigDecimal penalties,
        BigDecimal credits, BigDecimal paid, BigDecimal remissions, BigDecimal remaining,
        Instant orderIssuedAt
) {}
