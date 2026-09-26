package com.mparsict.vosolmaliat.application.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/** External Tax Organization contract for pushing a finalized debt paper. */
public record DebtPaperPushRequest(
        @NotBlank String nationalID,
        @NotBlank String xml,
        String hash,
        @NotNull OffsetDateTime produceDate,
        @JsonProperty("referenceID") @NotBlank String referenceId,
        @NotBlank String followCode,
        String secondParameter,
        @NotNull Integer year,
        @NotNull @Min(1) @Max(4) Integer period,
        @JsonProperty("taxOfficeID") @NotBlank String taxOfficeId,
        @JsonAlias("totalAmmount") String taxAmount,
        @NotBlank String taxSource,
        @NotBlank String announcementType
) {
    /** Compatibility for internal legacy fixtures; external JSON must provide announcementType. */
    public DebtPaperPushRequest(String nationalID, String xml, String hash, OffsetDateTime produceDate,
            String referenceId, String followCode, String secondParameter, Integer year, Integer period,
            Long taxOfficeId, String taxAmount, String taxSource) {
        this(nationalID, xml, hash, produceDate, referenceId, followCode, secondParameter, year, period,
                taxOfficeId == null ? null : taxOfficeId.toString(), taxAmount, taxSource, null);
    }
}
