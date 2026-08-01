package com.mparsict.vosolmaliat.application.dto.request.security;

import jakarta.validation.constraints.NotBlank;

/**
 * مشخصات مجوز جدید سامانه.
 *
 * @param code کد یکتای مجوز
 * @param description شرح کاربرد مجوز
 */
public record CreatePermissionRequest(
        @NotBlank String code,
        String description
) {}
