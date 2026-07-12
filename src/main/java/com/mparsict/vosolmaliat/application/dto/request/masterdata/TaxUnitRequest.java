package com.mparsict.vosolmaliat.application.dto.request.masterdata;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TaxUnitRequest(
        @NotNull Long taxAffairsOfficeId,
        @NotBlank String taxUnitName,
        String postalCode,
        String address,
        String phone
) {}
