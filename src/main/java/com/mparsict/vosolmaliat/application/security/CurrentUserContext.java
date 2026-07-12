package com.mparsict.vosolmaliat.application.security;

import com.mparsict.vosolmaliat.application.dto.response.security.MasterdataOrganizationResponse;
import com.mparsict.vosolmaliat.domain.enums.BusinessRole;

import java.util.Set;

public record CurrentUserContext(
        String keycloakUserId,
        String username,
        String email,
        String firstName,
        String lastName,
        BusinessRole businessRole,
        Set<String> roles,
        Set<String> groups,
        Set<String> permissions,
        MasterdataOrganizationResponse organization,
        boolean enabledInBusinessSystem,
        boolean lockedInBusinessSystem
) {
    public String fullName() {
        String value = String.join(" ",
                firstName == null ? "" : firstName,
                lastName == null ? "" : lastName).trim();
        return value.isBlank() ? username : value;
    }

    public boolean isAdmin() {
        return businessRole == BusinessRole.ADMIN || roles.contains("ADMIN") || groups.contains("ADMIN");
    }

    public boolean hasPermission(String permission) {
        return isAdmin() || permissions.contains(permission);
    }
}
