package com.mparsict.vosolmaliat.application.dto.response.masterdata;

/**
 * اطلاعات واحد مالیاتی برای نمایش یا انتخاب.
 *
 * @param id شناسه داخلی واحد
 * @param taxAffairsOfficeId شناسه اداره امور مالیاتی بالادست
 * @param taxAffairsOfficeName نام اداره امور مالیاتی بالادست
 * @param generalDirectorateOfTaxAffairsId شناسه اداره کل بالادست
 * @param generalDirectorateName نام اداره کل بالادست
 * @param taxUnitName نام واحد مالیاتی
 * @param postalCode کد پستی
 * @param address نشانی
 * @param phone شماره تماس
 */
public record TaxUnitResponse(
        Long id,
        Long taxAffairsOfficeId,
        String taxAffairsOfficeName,
        Long generalDirectorateOfTaxAffairsId,
        String generalDirectorateName,
        String taxUnitName,
        String postalCode,
        String address,
        String phone
) {}
