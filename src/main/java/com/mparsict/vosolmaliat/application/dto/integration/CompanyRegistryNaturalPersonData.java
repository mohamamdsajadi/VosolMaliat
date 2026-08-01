package com.mparsict.vosolmaliat.application.dto.integration;

/**
 * اطلاعات هویتی شخص حقیقی دریافتی از سامانه ثبت شرکت‌ها.
 *
 * @param nationalCode کد ملی شخص ایرانی
 * @param fida شناسه فراگیر شخص خارجی
 * @param firstName نام
 * @param lastName نام خانوادگی
 * @param fatherName نام پدر
 * @param birthDate تاریخ تولد
 */
public record CompanyRegistryNaturalPersonData(
        String nationalCode,
        String fida,
        String firstName,
        String lastName,
        String fatherName,
        String birthDate
) {
}
