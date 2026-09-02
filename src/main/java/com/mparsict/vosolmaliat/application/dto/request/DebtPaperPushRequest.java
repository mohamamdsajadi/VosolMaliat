package com.mparsict.vosolmaliat.application.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/** External Tax Organization contract for pushing a finalized debt paper. */
public record DebtPaperPushRequest(
        @NotBlank @Size(min = 10, max = 12) String nationalID,
        @NotBlank String xml,
        String hash,
        @NotNull OffsetDateTime produceDate,
        @JsonProperty("refrenceID") @NotBlank String referenceId,
        String followCode,
        String secondParameter,
        @NotNull Integer year,
        @NotNull @Min(1) @Max(4) Integer period,
        @JsonProperty("taxOfficeID") @NotNull Long taxOfficeId,
        String taxAmount,
        @NotBlank String taxSource
) {
}
