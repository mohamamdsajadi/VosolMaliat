package com.mparsict.vosolmaliat.application.dto.response.security;

/**
 * سلسله‌مراتب سازمانی منتسب به کاربر.
 *
 * @param generalDirectorateId شناسه اداره کل
 * @param generalDirectorateName نام اداره کل
 * @param taxAffairsOfficeId شناسه اداره امور مالیاتی
 * @param taxAffairsOfficeName نام اداره امور مالیاتی
 * @param taxUnitId شناسه واحد مالیاتی
 * @param taxUnitName نام واحد مالیاتی
 */
public record MasterdataOrganizationResponse(
        Long generalDirectorateId,
        String generalDirectorateName,
        Long taxAffairsOfficeId,
        String taxAffairsOfficeName,
        Long taxUnitId,
        String taxUnitName
) {}
