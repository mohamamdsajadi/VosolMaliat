package com.mparsict.vosolmaliat.application.dto.request.security;

import com.mparsict.vosolmaliat.domain.enums.BusinessRole;

public record UpdateAppUserProfileRequest(
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        BusinessRole businessRole,
        Long generalDirectorateId,
        Long taxAffairsOfficeId,
        Long taxUnitId
) {}
