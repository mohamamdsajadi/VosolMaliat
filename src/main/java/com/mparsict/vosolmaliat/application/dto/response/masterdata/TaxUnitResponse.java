package com.mparsict.vosolmaliat.application.dto.response.masterdata;

public record TaxUnitResponse(
        Long id,
        Long taxAffairsOfficeId,
        String taxAffairsOfficeName,
        Long generalDirectorateOfTaxAffairsId,
        String generalDirectorateName,
        String taxUnitName,
        String address,
        String phone
) {}
