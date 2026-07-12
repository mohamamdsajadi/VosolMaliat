package com.mparsict.vosolmaliat.application.dto.response.masterdata;

public record TaxAffairsOfficeResponse(
        Long id,
        Long generalDirectorateOfTaxAffairsId,
        String generalDirectorateName,
        String officeName,
        String postalCode,
        String address,
        String phone
) {}
