package com.mparsict.vosolmaliat.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record Tabsare3186TestStartRequest(
        @NotNull Long executionPaperId,
        @NotBlank String legalPersonNationalId,
        @NotBlank String debtPaperDate
) {
}
