package com.mparsict.vosolmaliat.application.dto.response.security;

import java.time.LocalDateTime;

public record AppUserProfileResponse(
        Long id,
        String keycloakUserId,
        String username,
        String email,
        String firstName,
        String lastName,
        String fullName,
        String phoneNumber,
        OrganizationUnitResponse organizationUnit,
        Boolean enabledInBusinessSystem,
        Boolean lockedInBusinessSystem,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
