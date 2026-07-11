package com.mparsict.vosolmaliat.application.service.security;

import com.mparsict.vosolmaliat.application.dto.request.security.*;
import com.mparsict.vosolmaliat.application.dto.response.security.AppUserProfileResponse;
import com.mparsict.vosolmaliat.common.exception.BusinessException;
import com.mparsict.vosolmaliat.domain.model.security.AppUserProfile;
import com.mparsict.vosolmaliat.domain.model.security.OrganizationUnit;
import com.mparsict.vosolmaliat.domain.repository.security.AppUserProfileRepository;
import com.mparsict.vosolmaliat.domain.repository.security.OrganizationUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppUserProfileService {

    private final AppUserProfileRepository userProfileRepository;
    private final OrganizationUnitRepository organizationUnitRepository;
    private final SecurityIdentityService securityIdentityService;
    private final AuditLogService auditLogService;

    @Transactional
    public AppUserProfileResponse create(CreateAppUserProfileRequest request) {
        validateUniqueForCreate(request);
        OrganizationUnit organizationUnit = request.organizationUnitId() == null ? null : findOrganizationUnit(request.organizationUnitId());
        AppUserProfile profile = AppUserProfile.builder()
                .keycloakUserId(request.keycloakUserId())
                .username(request.username())
                .email(blankToNull(request.email()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phoneNumber(request.phoneNumber())
                .organizationUnit(organizationUnit)
                .enabledInBusinessSystem(true)
                .lockedInBusinessSystem(false)
                .build();
        AppUserProfile saved = userProfileRepository.save(profile);
        auditLogService.record(securityIdentityService.currentUser(), "USER_PROFILE_CREATED", "APP_USER_PROFILE",
                saved.getId().toString(), null, saved.getUsername());
        return SecurityDtoMapper.toAppUserProfileResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AppUserProfileResponse> list() {
        return userProfileRepository.findAll().stream()
                .map(SecurityDtoMapper::toAppUserProfileResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AppUserProfileResponse get(Long id) {
        return SecurityDtoMapper.toAppUserProfileResponse(findProfile(id));
    }

    @Transactional
    public AppUserProfileResponse update(Long id, UpdateAppUserProfileRequest request) {
        AppUserProfile profile = findProfile(id);
        String oldValue = profile.getEmail() + "|" + profile.getFirstName() + "|" + profile.getLastName();
        if (request.email() != null && !request.email().equals(profile.getEmail()) && userProfileRepository.existsByEmail(request.email())) {
            throw new BusinessException("EMAIL_ALREADY_LINKED", "ایمیل قبلا به کاربر دیگری متصل شده است");
        }
        profile.setEmail(blankToNull(request.email()));
        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        profile.setPhoneNumber(request.phoneNumber());
        AppUserProfile saved = userProfileRepository.save(profile);
        auditLogService.record(securityIdentityService.currentUser(), "USER_PROFILE_UPDATED", "APP_USER_PROFILE",
                saved.getId().toString(), oldValue, saved.getEmail() + "|" + saved.getFirstName() + "|" + saved.getLastName());
        return SecurityDtoMapper.toAppUserProfileResponse(saved);
    }

    @Transactional
    public AppUserProfileResponse updateStatus(Long id, UserStatusRequest request) {
        AppUserProfile profile = findProfile(id);
        Boolean oldValue = profile.getEnabledInBusinessSystem();
        profile.setEnabledInBusinessSystem(request.enabled());
        AppUserProfile saved = userProfileRepository.save(profile);
        auditLogService.record(securityIdentityService.currentUser(), "USER_PROFILE_STATUS_CHANGED", "APP_USER_PROFILE",
                saved.getId().toString(), String.valueOf(oldValue), String.valueOf(saved.getEnabledInBusinessSystem()));
        return SecurityDtoMapper.toAppUserProfileResponse(saved);
    }

    @Transactional
    public AppUserProfileResponse updateLock(Long id, UserLockRequest request) {
        AppUserProfile profile = findProfile(id);
        Boolean oldValue = profile.getLockedInBusinessSystem();
        profile.setLockedInBusinessSystem(request.locked());
        AppUserProfile saved = userProfileRepository.save(profile);
        auditLogService.record(securityIdentityService.currentUser(), "USER_PROFILE_LOCK_CHANGED", "APP_USER_PROFILE",
                saved.getId().toString(), String.valueOf(oldValue), String.valueOf(saved.getLockedInBusinessSystem()));
        return SecurityDtoMapper.toAppUserProfileResponse(saved);
    }

    @Transactional
    public AppUserProfileResponse assignOrganization(Long id, AssignOrganizationUnitRequest request) {
        AppUserProfile profile = findProfile(id);
        Long oldValue = profile.getOrganizationUnit() == null ? null : profile.getOrganizationUnit().getId();
        OrganizationUnit organizationUnit = findOrganizationUnit(request.organizationUnitId());
        profile.setOrganizationUnit(organizationUnit);
        AppUserProfile saved = userProfileRepository.save(profile);
        auditLogService.record(securityIdentityService.currentUser(), "USER_PROFILE_ORGANIZATION_ASSIGNED", "APP_USER_PROFILE",
                saved.getId().toString(), String.valueOf(oldValue), organizationUnit.getId().toString());
        return SecurityDtoMapper.toAppUserProfileResponse(saved);
    }

    private AppUserProfile findProfile(Long id) {
        return userProfileRepository.findById(id)
                .orElseThrow(() -> new BusinessException("USER_PROFILE_NOT_FOUND", "پروفایل کاربر یافت نشد"));
    }

    private OrganizationUnit findOrganizationUnit(Long id) {
        return organizationUnitRepository.findById(id)
                .orElseThrow(() -> new BusinessException("ORGANIZATION_UNIT_NOT_FOUND", "واحد سازمانی یافت نشد"));
    }

    private void validateUniqueForCreate(CreateAppUserProfileRequest request) {
        if (userProfileRepository.existsByKeycloakUserId(request.keycloakUserId())) {
            throw new BusinessException("KEYCLOAK_USER_ALREADY_LINKED", "این کاربر Keycloak قبلا متصل شده است");
        }
        if (userProfileRepository.existsByUsername(request.username())) {
            throw new BusinessException("USERNAME_ALREADY_LINKED", "نام کاربری قبلا متصل شده است");
        }
        if (request.email() != null && !request.email().isBlank() && userProfileRepository.existsByEmail(request.email())) {
            throw new BusinessException("EMAIL_ALREADY_LINKED", "ایمیل قبلا متصل شده است");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
