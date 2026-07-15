package com.mparsict.vosolmaliat.application.dto.response;

import java.util.Map;

public record ExecutionPaperDetailResponse(
        ExecutionPaperDto summary,
        Map<String, Object> executionPaper,
        Map<String, Object> finalizedDebtPaper,
        Map<String, Object> taxpayer,
        Map<String, Object> debtInformation,
        Map<String, Object> notification,
        Map<String, Object> ruleRejection
) {}
