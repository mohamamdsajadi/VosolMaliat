package com.mparsict.vosolmaliat.application.service.security;

import com.mparsict.vosolmaliat.application.dto.response.security.AppUserProfileResponse;
import com.mparsict.vosolmaliat.application.dto.response.security.GroupPermissionResponse;
import com.mparsict.vosolmaliat.application.dto.response.security.OrganizationUnitResponse;
import com.mparsict.vosolmaliat.application.dto.response.security.PermissionResponse;
import com.mparsict.vosolmaliat.domain.model.security.AppUserProfile;
import com.mparsict.vosolmaliat.domain.model.security.GroupPermission;
import com.mparsict.vosolmaliat.domain.model.security.OrganizationUnit;
import com.mparsict.vosolmaliat.domain.model.security.Permission;

public final class SecurityDtoMapper {
    private SecurityDtoMapper() {
    }

    public static OrganizationUnitResponse toOrganizationUnitResponse(OrganizationUnit entity) {
        if (entity == null) {
            return null;
        }
        return new OrganizationUnitResponse(
                entity.getId(),
                entity.getName(),
                entity.getCode(),
                entity.getType().name(),
                entity.getParent() == null ? null : entity.getParent().getId(),
                entity.getEnabled()
        );
    }

    public static AppUserProfileResponse toAppUserProfileResponse(AppUserProfile entity) {
        return new AppUserProfileResponse(
                entity.getId(),
                entity.getKeycloakUserId(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getFirstName(),
                entity.getLastName(),
                fullName(entity.getFirstName(), entity.getLastName(), entity.getUsername()),
                entity.getPhoneNumber(),
                toOrganizationUnitResponse(entity.getOrganizationUnit()),
                entity.getEnabledInBusinessSystem(),
                entity.getLockedInBusinessSystem(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static PermissionResponse toPermissionResponse(Permission entity) {
        return new PermissionResponse(entity.getId(), entity.getCode(), entity.getDescription(), entity.getEnabled());
    }

    public static GroupPermissionResponse toGroupPermissionResponse(GroupPermission entity) {
        return new GroupPermissionResponse(entity.getId(), entity.getGroupName(), toPermissionResponse(entity.getPermission()));
    }

    private static String fullName(String firstName, String lastName, String username) {
        String value = ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
        return value.isBlank() ? username : value;
    }
}
