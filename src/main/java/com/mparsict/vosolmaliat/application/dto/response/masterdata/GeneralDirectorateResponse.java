package com.mparsict.vosolmaliat.application.dto.response.masterdata;

/**
 * اطلاعات اداره کل مالیاتی برای نمایش یا انتخاب.
 *
 * @param id شناسه داخلی اداره کل
 * @param name نام اداره کل
 * @param postalCode کد پستی
 * @param address نشانی
 * @param phone شماره تماس
 */
public record GeneralDirectorateResponse(
        Long id,
        String name,
        String postalCode,
        String address,
        String phone
) {}
