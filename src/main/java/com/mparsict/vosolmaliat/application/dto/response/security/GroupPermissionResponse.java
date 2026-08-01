package com.mparsict.vosolmaliat.application.dto.response.security;

/**
 * اتصال ثبت‌شده بین گروه کاربری و مجوز.
 *
 * @param id شناسه داخلی اتصال
 * @param groupName نام گروه کاربری
 * @param permission مشخصات مجوز متصل
 */
public record GroupPermissionResponse(
        Long id,
        String groupName,
        PermissionResponse permission
) {}
