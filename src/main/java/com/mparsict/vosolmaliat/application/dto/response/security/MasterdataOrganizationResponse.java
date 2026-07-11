package com.mparsict.vosolmaliat.application.dto.response.security;

public record MasterdataOrganizationResponse(
        Long generalDirectorateId,
        String generalDirectorateName,
        Long taxAffairsOfficeId,
        String taxAffairsOfficeName,
        Long taxUnitId,
        String taxUnitName
) {}
