package com.mparsict.vosolmaliat.application.dto.response.cartable;

/**
 * کاربر مجاز به دریافت وظیفه واگذارشده.
 *
 * @param userProfileId شناسه پروفایل کاربر
 * @param username نام کاربری
 * @param fullName نام کامل
 * @param businessRole نقش سازمانی
 */
public record DelegationTargetResponse(
        Long userProfileId,
        String username,
        String fullName,
        String businessRole
) {
}
