package com.mparsict.vosolmaliat.application.service.security;

import com.mparsict.vosolmaliat.application.dto.response.security.CurrentUserResponse;
import com.mparsict.vosolmaliat.application.security.CurrentUserContext;
import com.mparsict.vosolmaliat.application.security.KeycloakPrincipalMapper;
import com.mparsict.vosolmaliat.common.exception.BusinessException;
import com.mparsict.vosolmaliat.domain.enums.BusinessRole;
import com.mparsict.vosolmaliat.domain.model.security.AppUserProfile;
import com.mparsict.vosolmaliat.domain.repository.security.AppUserProfileRepository;
import com.mparsict.vosolmaliat.domain.repository.security.GroupPermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SecurityIdentityService {

    private final KeycloakPrincipalMapper principalMapper;
    private final AppUserProfileRepository userProfileRepository;
    private final GroupPermissionRepository groupPermissionRepository;

    @Transactional(readOnly = true)
    public CurrentUserContext currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new BusinessException("INVALID_OR_MISSING_JWT", "توکن احراز هویت معتبر نیست");
        }

        Set<String> roles = new TreeSet<>(principalMapper.extractRealmRoles(jwt));
        roles.addAll(principalMapper.extractClientRoles(jwt));
        Set<String> groups = principalMapper.extractGroups(jwt);
        AppUserProfile profile = userProfileRepository.findByKeycloakUserId(jwt.getSubject()).orElse(null);
        BusinessRole businessRole = profile == null || profile.getBusinessRole() == null ? inferBusinessRole(roles, groups) : profile.getBusinessRole();
        Set<String> identityGroups = new TreeSet<>(roles);
        identityGroups.addAll(groups);
        if (businessRole != null) {
            identityGroups.add(businessRole.name());
        }
        Set<String> permissions = groupPermissionRepository.findByGroupNameIn(identityGroups).stream()
                .filter(groupPermission -> Boolean.TRUE.equals(groupPermission.getPermission().getEnabled()))
                .map(groupPermission -> groupPermission.getPermission().getCode())
                .collect(Collectors.toCollection(TreeSet::new));

        if (profile != null && Boolean.FALSE.equals(profile.getEnabledInBusinessSystem())) {
            throw new BusinessException("BUSINESS_USER_DISABLED", "کاربر در سامانه کسب‌وکاری غیرفعال است");
        }
        if (profile != null && Boolean.TRUE.equals(profile.getLockedInBusinessSystem())) {
            throw new BusinessException("BUSINESS_USER_LOCKED", "کاربر در سامانه کسب‌وکاری قفل شده است");
        }

        return new CurrentUserContext(
                jwt.getSubject(),
                principalMapper.username(jwt),
                firstNonBlank(profile == null ? null : profile.getEmail(), jwt.getClaimAsString("email")),
                firstNonBlank(profile == null ? null : profile.getFirstName(), jwt.getClaimAsString("given_name")),
                firstNonBlank(profile == null ? null : profile.getLastName(), jwt.getClaimAsString("family_name")),
                businessRole,
                roles,
                groups,
                permissions,
                SecurityDtoMapper.toOrganizationResponse(profile),
                profile == null || Boolean.TRUE.equals(profile.getEnabledInBusinessSystem()),
                profile != null && Boolean.TRUE.equals(profile.getLockedInBusinessSystem())
        );
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse currentUserResponse() {
        CurrentUserContext user = currentUser();
        Set<String> allowedActions = new TreeSet<>(user.permissions());
        if (user.isAdmin()) {
            allowedActions.add("ADMIN");
        }
        return new CurrentUserResponse(
                user.keycloakUserId(),
                user.username(),
                user.fullName(),
                user.email(),
                user.businessRole() == null ? null : user.businessRole().name(),
                user.roles(),
                user.groups(),
                user.permissions(),
                user.organization(),
                user.enabledInBusinessSystem(),
                user.lockedInBusinessSystem(),
                allowedActions
        );
    }

    private String firstNonBlank(String preferred, String fallback) {
        return preferred != null && !preferred.isBlank() ? preferred : fallback;
    }

    private BusinessRole inferBusinessRole(Set<String> roles, Set<String> groups) {
        for (BusinessRole role : BusinessRole.values()) {
            if (roles.contains(role.name()) || groups.contains(role.name())) {
                return role;
            }
        }
        return null;
    }
}
