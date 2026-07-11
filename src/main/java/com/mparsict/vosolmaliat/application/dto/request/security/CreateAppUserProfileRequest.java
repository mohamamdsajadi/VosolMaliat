package com.mparsict.vosolmaliat.application.dto.request.security;

import jakarta.validation.constraints.NotBlank;

public record CreateAppUserProfileRequest(
        @NotBlank String keycloakUserId,
        @NotBlank String username,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        Long organizationUnitId
) {}
