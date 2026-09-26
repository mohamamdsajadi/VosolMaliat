package com.mparsict.vosolmaliat.application.dto.response.security;

import java.util.Set;

/**
 * اطلاعات هویتی و دسترسی‌های کاربر جاری.
 *
 * @param keycloakUserId شناسه کاربر در Keycloak
 * @param username نام کاربری
 * @param fullName نام کامل
 * @param email نشانی ایمیل
 * @param businessRole نقش سازمانی
 * @param keycloakRoles نقش‌های دریافت‌شده از Keycloak
 * @param keycloakGroups گروه‌های دریافت‌شده از Keycloak
 * @param localBusinessPermissions مجوزهای تجاری محلی
 * @param organization ساختار سازمانی محل خدمت
 * @param enabledInBusinessSystem آیا کاربر در سامانه فعال است
 * @param lockedInBusinessSystem آیا کاربر در سامانه قفل است
 * @param allowedActions عملیات مجاز رابط کاربری
 */
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
