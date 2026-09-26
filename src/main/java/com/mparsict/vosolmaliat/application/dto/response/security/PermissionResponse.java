package com.mparsict.vosolmaliat.application.dto.response.security;

/**
 * مشخصات مجوز قابل استفاده در کنترل دسترسی.
 *
 * @param id شناسه داخلی مجوز
 * @param code کد یکتای مجوز
 * @param description شرح کاربرد مجوز
 * @param enabled آیا مجوز فعال است
 */
public record PermissionResponse(
        Long id,
        String code,
        String description,
        Boolean enabled
) {}
