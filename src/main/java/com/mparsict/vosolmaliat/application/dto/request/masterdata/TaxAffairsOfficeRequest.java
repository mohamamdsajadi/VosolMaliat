package com.mparsict.vosolmaliat.application.dto.request.masterdata;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TaxAffairsOfficeRequest(
        @NotNull Long generalDirectorateOfTaxAffairsId,
        @NotBlank String officeName
) {}
