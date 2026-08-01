package com.mparsict.vosolmaliat.application.dto.integration;

import com.mparsict.vosolmaliat.domain.enums.RelatedPersonPostCode;
import com.mparsict.vosolmaliat.domain.enums.RelatedPersonType;

/**
 * اطلاعات شخص مرتبط و سمت او در شرکت، دریافتی از سامانه ثبت شرکت‌ها.
 *
 * @param sourceRecordId شناسه رکورد در سامانه مبدأ
 * @param personType نوع شخص حقیقی یا حقوقی
 * @param postCode کد سمت شخص در شرکت
 * @param startDate تاریخ شروع اعتبار سمت
 * @param endDate تاریخ پایان اعتبار سمت؛ برای سمت فعال خالی است
 * @param hasSignatoryAuthority آیا شخص حق امضا دارد
 * @param nationalCode کد ملی شخص حقیقی ایرانی
 * @param fida شناسه فراگیر شخص حقیقی خارجی
 * @param firstName نام شخص حقیقی
 * @param lastName نام خانوادگی شخص حقیقی
 * @param fatherName نام پدر شخص حقیقی
 * @param birthDate تاریخ تولد شخص حقیقی
 * @param nationalId شناسه ملی شخص حقوقی
 * @param registrationNumber شماره ثبت شخص حقوقی
 * @param registrationDate تاریخ ثبت شخص حقوقی
 * @param companyName نام شخص حقوقی
 * @param legalRepresentative نماینده حقیقی شخص حقوقی
 */
public record CompanyRegistryRelatedPersonData(
        String sourceRecordId,
        RelatedPersonType personType,
        RelatedPersonPostCode postCode,
        String startDate,
        String endDate,
        boolean hasSignatoryAuthority,
        String nationalCode,
        String fida,
        String firstName,
        String lastName,
        String fatherName,
        String birthDate,
        String nationalId,
        String registrationNumber,
        String registrationDate,
        String companyName,
        CompanyRegistryNaturalPersonData legalRepresentative
) {
}
