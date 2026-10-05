package com.mparsict.vosolmaliat.application.dto.request.admin;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import com.mparsict.vosolmaliat.domain.model.sanim.SanimAmounts;
public record ManualExecutionPaperRequest(
        String executionPaperUniqueCode, @NotNull Long finalTaxDebtPaperId, @NotNull Long taxpayerId,
        Long mainTaxBureauId, Long taxBureauId,
        @NotBlank String taxSource, String currentState, @NotNull @Valid SanimAmounts currentAmounts
) {}
