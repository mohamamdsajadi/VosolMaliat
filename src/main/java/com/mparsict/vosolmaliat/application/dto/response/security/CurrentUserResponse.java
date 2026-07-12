package com.mparsict.vosolmaliat.application.dto.response.security;

import java.util.Set;

public record CurrentUserResponse(
        String keycloakUserId,
        String username,
        String fullName,
        String email,
        String businessRole,
        Set<String> keycloakRoles,
        Set<String> keycloakGroups,
        Set<String> localBusinessPermissions,
        MasterdataOrganizationResponse organization,
        Boolean enabledInBusinessSystem,
        Boolean lockedInBusinessSystem,
        Set<String> allowedActions
) {}
