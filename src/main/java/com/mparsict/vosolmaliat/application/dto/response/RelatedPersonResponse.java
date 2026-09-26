package com.mparsict.vosolmaliat.application.dto.response;

/**
 * اطلاعات قابل نمایش شخص مرتبط و وضعیت سمت او.
 *
 * @param relationshipId شناسه رابطه شخص با مؤدی
 * @param personType نوع شخص حقیقی یا حقوقی
 * @param postId شناسه داخلی سمت
 * @param postCode کد سمت
 * @param postLabel عنوان فارسی سمت
 * @param startDate تاریخ شروع سمت
 * @param endDate تاریخ پایان سمت
 * @param hasSignatoryAuthority آیا شخص حق امضا دارد
 * @param naturalPersonId شناسه داخلی شخص حقیقی
 * @param nationalCode کد ملی شخص حقیقی ایرانی
 * @param fida شناسه فراگیر شخص حقیقی خارجی
 * @param firstName نام شخص حقیقی
 * @param lastName نام خانوادگی شخص حقیقی
 * @param fatherName نام پدر شخص حقیقی
 * @param birthDate تاریخ تولد شخص حقیقی
 * @param legalPersonId شناسه داخلی شخص حقوقی
 * @param nationalId شناسه ملی شخص حقوقی
 * @param registrationNumber شماره ثبت شخص حقوقی
 * @param registrationDate تاریخ ثبت شخص حقوقی
 * @param companyName نام شخص حقوقی
 * @param legalRepresentativeNaturalPersonId شناسه داخلی نماینده حقیقی شخص حقوقی
 * @param legalRepresentativeNationalCode کد ملی نماینده حقیقی
 * @param legalRepresentativeFida شناسه فراگیر نماینده حقیقی خارجی
 * @param legalRepresentativeFirstName نام نماینده حقیقی
 * @param legalRepresentativeLastName نام خانوادگی نماینده حقیقی
 * @param legalRepresentativeFatherName نام پدر نماینده حقیقی
 * @param legalRepresentativeBirthDate تاریخ تولد نماینده حقیقی
 */
public record RelatedPersonResponse(
        Long relationshipId,
        String personType,
        Long postId,
        String postCode,
        String postLabel,
        String startDate,
        String endDate,
        boolean hasSignatoryAuthority,
        Long naturalPersonId,
        String nationalCode,
        String fida,
        String firstName,
        String lastName,
        String fatherName,
        String birthDate,
        Long legalPersonId,
        String nationalId,
        String registrationNumber,
        String registrationDate,
        String companyName,
        Long legalRepresentativeNaturalPersonId,
        String legalRepresentativeNationalCode,
        String legalRepresentativeFida,
        String legalRepresentativeFirstName,
        String legalRepresentativeLastName,
        String legalRepresentativeFatherName,
        String legalRepresentativeBirthDate
) {
}
