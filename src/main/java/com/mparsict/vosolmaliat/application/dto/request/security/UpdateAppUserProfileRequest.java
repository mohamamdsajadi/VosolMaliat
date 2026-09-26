package com.mparsict.vosolmaliat.application.dto.request.security;

import com.mparsict.vosolmaliat.domain.enums.BusinessRole;

/**
 * اطلاعات قابل ویرایش پروفایل کاربر سامانه.
 *
 * @param email نشانی ایمیل
 * @param firstName نام
 * @param lastName نام خانوادگی
 * @param phoneNumber شماره همراه
 * @param businessRole نقش سازمانی کاربر
 * @param generalDirectorateId شناسه اداره کل محل خدمت
 * @param taxAffairsOfficeId شناسه اداره امور مالیاتی محل خدمت
 * @param taxUnitId شناسه واحد مالیاتی محل خدمت
 */
public record UpdateAppUserProfileRequest(
        String email,
        String firstName,
        String lastName,
        String phoneNumber,
        BusinessRole businessRole,
        Long generalDirectorateId,
        Long taxAffairsOfficeId,
        Long taxUnitId
) {}
