package com.mparsict.vosolmaliat.application.dto.request.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.mparsict.vosolmaliat.domain.enums.BusinessRole;

public record CreateAppUserProfileRequest(
        @NotBlank String keycloakUserId,
        @NotBlank String username,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        @NotNull BusinessRole businessRole,
        Long generalDirectorateId,
        Long taxAffairsOfficeId,
        Long taxUnitId
) {}
