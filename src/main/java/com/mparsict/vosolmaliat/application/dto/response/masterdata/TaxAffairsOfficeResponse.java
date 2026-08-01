package com.mparsict.vosolmaliat.application.dto.response.masterdata;

/**
 * اطلاعات اداره امور مالیاتی برای نمایش یا انتخاب.
 *
 * @param id شناسه داخلی اداره
 * @param generalDirectorateOfTaxAffairsId شناسه اداره کل بالادست
 * @param generalDirectorateName نام اداره کل بالادست
 * @param officeName نام اداره امور مالیاتی
 * @param postalCode کد پستی
 * @param address نشانی
 * @param phone شماره تماس
 */
public record TaxAffairsOfficeResponse(
        Long id,
        Long generalDirectorateOfTaxAffairsId,
        String generalDirectorateName,
        String officeName,
        String postalCode,
        String address,
        String phone
) {}
