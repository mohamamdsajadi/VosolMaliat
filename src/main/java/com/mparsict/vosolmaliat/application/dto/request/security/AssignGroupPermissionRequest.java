package com.mparsict.vosolmaliat.application.dto.request.security;

import jakarta.validation.constraints.NotBlank;

/**
 * اتصال یک مجوز به گروه کاربری.
 *
 * @param groupName نام گروه کاربری
 * @param permissionCode کد مجوز
 */
public record AssignGroupPermissionRequest(
        @NotBlank String groupName,
        @NotBlank String permissionCode
) {}
