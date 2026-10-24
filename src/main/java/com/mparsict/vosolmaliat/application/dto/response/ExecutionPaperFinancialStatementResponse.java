package com.mparsict.vosolmaliat.application.dto.response;

import java.util.List;
import java.util.Map;

/** Stored financial snapshots are cumulative balances, not individual payment receipts. */
public record ExecutionPaperFinancialStatementResponse(
        ExecutionPaperDetailResponse detail,
        List<Map<String, Object>> history
) {}
