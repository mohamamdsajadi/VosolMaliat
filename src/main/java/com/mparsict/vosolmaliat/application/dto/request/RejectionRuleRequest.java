package com.mparsict.vosolmaliat.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RejectionRuleRequest(
        @NotBlank String ruleJson,
        @NotBlank String startDate,
        @NotBlank String endDate,
        String documentNo,
        String filePath,
        String description
) {}
