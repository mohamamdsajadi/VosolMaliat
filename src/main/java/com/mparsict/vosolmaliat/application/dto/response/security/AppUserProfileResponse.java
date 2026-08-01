package com.mparsict.vosolmaliat.application.dto.response.security;

import java.time.LocalDateTime;

/**
 * پروفایل، نقش و سازمان منتسب به کاربر.
 *
 * @param id شناسه داخلی پروفایل
 * @param keycloakUserId شناسه کاربر در Keycloak
 * @param username نام کاربری
 * @param email نشانی ایمیل
 * @param firstName نام
 * @param lastName نام خانوادگی
 * @param fullName نام کامل
 * @param phoneNumber شماره همراه
 * @param businessRole نقش سازمانی
 * @param organization ساختار سازمانی محل خدمت
 * @param enabledInBusinessSystem آیا کاربر در سامانه فعال است
 * @param lockedInBusinessSystem آیا کاربر در سامانه قفل است
 * @param createdAt زمان ایجاد پروفایل
 * @param updatedAt زمان آخرین ویرایش پروفایل
 */
public record AppUserProfileResponse(
        Long id,
        String keycloakUserId,
        String username,
        String email,
        String firstName,
        String lastName,
        String fullName,
        String phoneNumber,
        String businessRole,
        MasterdataOrganizationResponse organization,
        Boolean enabledInBusinessSystem,
        Boolean lockedInBusinessSystem,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
