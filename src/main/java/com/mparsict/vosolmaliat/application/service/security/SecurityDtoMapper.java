package com.mparsict.vosolmaliat.application.service.security;

import com.mparsict.vosolmaliat.application.dto.response.security.AppUserProfileResponse;
import com.mparsict.vosolmaliat.application.dto.response.security.GroupPermissionResponse;
import com.mparsict.vosolmaliat.application.dto.response.security.MasterdataOrganizationResponse;
import com.mparsict.vosolmaliat.application.dto.response.security.PermissionResponse;
import com.mparsict.vosolmaliat.domain.model.security.AppUserProfile;
import com.mparsict.vosolmaliat.domain.model.security.GroupPermission;
import com.mparsict.vosolmaliat.domain.model.security.Permission;

public final class SecurityDtoMapper {
    private SecurityDtoMapper() {
    }

    public static MasterdataOrganizationResponse toOrganizationResponse(AppUserProfile profile) {
        if (profile == null) {
            return null;
        }
        return new MasterdataOrganizationResponse(
                profile.getGeneralDirectorateId(),
                profile.getGeneralDirectorate() == null ? null : profile.getGeneralDirectorate().getGeneralDirectorateName(),
                profile.getTaxAffairsOfficeId(),
                profile.getTaxAffairsOffice() == null ? null : profile.getTaxAffairsOffice().getOfficeName()
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
                entity.getBusinessRole() == null ? null : entity.getBusinessRole().name(),
                toOrganizationResponse(entity),
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
