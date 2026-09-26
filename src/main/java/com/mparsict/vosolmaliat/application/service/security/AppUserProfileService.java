package com.mparsict.vosolmaliat.application.service.security;

import com.mparsict.vosolmaliat.application.dto.request.security.*;
import com.mparsict.vosolmaliat.application.dto.response.security.AppUserProfileResponse;
import com.mparsict.vosolmaliat.common.exception.BusinessException;
import com.mparsict.vosolmaliat.domain.model.masterdata.GeneralDirectorateOfTaxAffairs;
import com.mparsict.vosolmaliat.domain.model.masterdata.TaxAffairsOffice;
import com.mparsict.vosolmaliat.domain.model.masterdata.TaxUnit;
import com.mparsict.vosolmaliat.domain.model.security.AppUserProfile;
import com.mparsict.vosolmaliat.domain.repository.TaxUnitRepository;
import com.mparsict.vosolmaliat.domain.repository.security.AppUserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppUserProfileService {

    private final AppUserProfileRepository userProfileRepository;
    private final TaxUnitRepository taxUnitRepository;
    private final SecurityIdentityService securityIdentityService;
    private final AuditLogService auditLogService;

    @Transactional
    public AppUserProfileResponse create(CreateAppUserProfileRequest request) {
        validateUniqueForCreate(request);
        TaxUnit taxUnit = request.taxUnitId() == null ? null : findTaxUnit(request.taxUnitId());
        AppUserProfile profile = AppUserProfile.builder()
                .keycloakUserId(request.keycloakUserId())
                .username(request.username())
                .email(blankToNull(request.email()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phoneNumber(request.phoneNumber())
                .businessRole(request.businessRole())
                .generalDirectorate(generalReference(request.generalDirectorateId()))
                .taxAffairsOffice(taxAffairsOfficeReference(request.taxAffairsOfficeId()))
                .taxUnit(taxUnit)
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
        String oldValue = profile.getEmail() + "|" + profile.getFirstName() + "|" + profile.getLastName() + "|" + profile.getBusinessRole();
        if (request.email() != null && !request.email().equals(profile.getEmail()) && userProfileRepository.existsByEmail(request.email())) {
            throw new BusinessException("EMAIL_ALREADY_LINKED", "ایمیل قبلا به کاربر دیگری متصل شده است");
        }
        profile.setEmail(blankToNull(request.email()));
        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        profile.setPhoneNumber(request.phoneNumber());
        if (request.businessRole() != null) {
            profile.setBusinessRole(request.businessRole());
        }
        profile.setGeneralDirectorate(generalReference(request.generalDirectorateId()));
        profile.setTaxAffairsOffice(taxAffairsOfficeReference(request.taxAffairsOfficeId()));
        TaxUnit taxUnit = request.taxUnitId() == null ? null : findTaxUnit(request.taxUnitId());
        profile.setTaxUnit(taxUnit);
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
    public AppUserProfileResponse assignTaxUnit(Long id, AssignTaxUnitRequest request) {
        AppUserProfile profile = findProfile(id);
        Long oldValue = profile.getTaxUnit() == null ? null : profile.getTaxUnit().getId();
        TaxUnit taxUnit = findTaxUnit(request.taxUnitId());
        profile.setTaxUnit(taxUnit);
        AppUserProfile saved = userProfileRepository.save(profile);
        auditLogService.record(securityIdentityService.currentUser(), "USER_PROFILE_TAX_UNIT_ASSIGNED", "APP_USER_PROFILE",
                saved.getId().toString(), String.valueOf(oldValue), taxUnit.getId().toString());
        return SecurityDtoMapper.toAppUserProfileResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        AppUserProfile profile = findProfile(id);
        userProfileRepository.delete(profile);
        auditLogService.record(securityIdentityService.currentUser(), "USER_PROFILE_DELETED", "APP_USER_PROFILE",
                id.toString(), profile.getUsername(), null);
    }

    private AppUserProfile findProfile(Long id) {
        return userProfileRepository.findById(id)
                .orElseThrow(() -> new BusinessException("USER_PROFILE_NOT_FOUND", "پروفایل کاربر یافت نشد"));
    }

    private TaxUnit findTaxUnit(Long id) {
        return taxUnitRepository.findById(id)
                .orElseThrow(() -> new BusinessException("TAX_UNIT_NOT_FOUND", "واحد مالیاتی یافت نشد"));
    }

    private GeneralDirectorateOfTaxAffairs generalReference(Long id) {
        return id == null ? null : GeneralDirectorateOfTaxAffairs.builder().id(id).build();
    }

    private TaxAffairsOffice taxAffairsOfficeReference(Long id) {
        return id == null ? null : TaxAffairsOffice.builder().id(id).build();
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
