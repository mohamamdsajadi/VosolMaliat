package com.mparsict.vosolmaliat.application.dto.request.masterdata;

import jakarta.validation.constraints.NotBlank;

/**
 * اطلاعات ایجاد یا ویرایش اداره کل مالیاتی.
 *
 * @param name نام اداره کل
 * @param postalCode کد پستی
 * @param address نشانی
 * @param phone شماره تماس
 */
public record GeneralDirectorateRequest(
        @NotBlank String name,
        @NotBlank(message="کد پستی الزامی است") String postalCode,
        @NotBlank(message="نشانی الزامی است") String address,
        @NotBlank(message="شماره تلفن الزامی است") String phone
) {}
