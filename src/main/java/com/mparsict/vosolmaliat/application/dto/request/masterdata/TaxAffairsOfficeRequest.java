package com.mparsict.vosolmaliat.application.dto.request.masterdata;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * اطلاعات ایجاد یا ویرایش اداره امور مالیاتی.
 *
 * @param generalDirectorateOfTaxAffairsId شناسه اداره کل بالادست
 * @param officeName نام اداره امور مالیاتی
 * @param postalCode کد پستی
 * @param address نشانی
 * @param phone شماره تماس
 */
public record TaxAffairsOfficeRequest(
        @NotNull Long generalDirectorateOfTaxAffairsId,
        @NotBlank String officeName,
        String postalCode,
        String address,
        String phone,
        @jakarta.validation.constraints.Min(0) @jakarta.validation.constraints.Max(1) Integer officeType,
        String officeCode
) {}
