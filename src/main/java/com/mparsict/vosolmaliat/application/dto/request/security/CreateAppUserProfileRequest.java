package com.mparsict.vosolmaliat.application.dto.request.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.mparsict.vosolmaliat.domain.enums.BusinessRole;

/**
 * اطلاعات لازم برای ایجاد پروفایل کاربر سامانه.
 *
 * @param keycloakUserId شناسه کاربر در Keycloak
 * @param username نام کاربری
 * @param email نشانی ایمیل
 * @param firstName نام
 * @param lastName نام خانوادگی
 * @param phoneNumber شماره همراه
 * @param businessRole نقش سازمانی کاربر
 * @param generalDirectorateId شناسه اداره کل محل خدمت
 * @param taxAffairsOfficeId شناسه اداره امور مالیاتی محل خدمت
 * @param taxUnitId شناسه واحد مالیاتی محل خدمت
 */
public record CreateAppUserProfileRequest(
        @NotBlank String keycloakUserId,
        @NotBlank String username,
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        @NotNull BusinessRole businessRole,
        Long generalDirectorateId,
        Long taxAffairsOfficeId,
        Long taxUnitId
) {}
