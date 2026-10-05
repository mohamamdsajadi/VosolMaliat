package com.mparsict.vosolmaliat.application.service;

import com.mparsict.vosolmaliat.application.dto.request.DebtPaperIssuanceRequest;
import com.mparsict.vosolmaliat.domain.enums.*;
import com.mparsict.vosolmaliat.domain.value.PersianDates;
import com.mparsict.vosolmaliat.application.dto.request.SanimDocumentDetails;
import java.util.*;

/** Validates the documented contract before starting a workflow or persisting business identities. */
public final class ModelRequestValidation {
    private ModelRequestValidation() {}
    public static void requireRequest(DebtPaperIssuanceRequest request) {
        if (request == null) throw SanimXmlValues.invalid("اطلاعات درخواست الزامی است.");
        List<String> missing = new ArrayList<>();
        required(missing, request.debtPaperUniqueCode(), "شناسه یکتای برگه بدهی قطعی");
        required(missing, request.taxSource(), "منبع مالیاتی");
        var d = request.sourceDocument(); var person = request.taxpayer();
        if (d == null) missing.add("اطلاعات برگه بدهی قطعی");
        if (person == null) missing.add("اطلاعات مودی");
        fail(missing);
        required(missing, d.getTaxpayerIdentifier(), "شناسه مودی (TIN)");
        required(missing, d.getRegistrationTraceCode(), "کد رهگیری ثبت‌نام");
        required(missing, d.getIssuanceReason(), "علت قطعیت");
        required(missing, d.getBasisDocumentDateRaw(), "تاریخ صدور برگه بدهی قطعی");
        required(missing, d.getDocumentNumber(), "شماره ابلاغ برگه بدهی قطعی");
        required(missing, person.postalCode(), "کد پستی");
        required(missing, person.address(), "اقامتگاه قانونی");
        if (request.taxBureauId() == null && blank(d.getSourceTaxOfficeCode())) missing.add("اداره امور مالیاتی");
        if ("NATURAL".equals(person.type())) {
            if (blank(person.nationalCode()) == blank(person.fida()))
                throw SanimXmlValues.invalid("برای شخص حقیقی دقیقاً یکی از کد ملی یا فیدا الزامی است.");
            required(missing, d.getTaxpayerName(), "نام کامل شخص حقیقی");
        } else if ("LEGAL".equals(person.type())) {
            required(missing, person.nationalId(), "شناسه ملی شخص حقوقی");
            required(missing, person.companyName(), "نام شخص حقوقی");
        } else throw SanimXmlValues.invalid("نوع مودی باید حقیقی یا حقوقی باشد.");
        fail(missing);
        try {
            d.setIssuanceReason(FinalizationReason.fromSourceValue(d.getIssuanceReason()).name());
            TaxSourceType.fromSourceValue(request.taxSource());
            PersianDates.parse(d.getBasisDocumentDateRaw());
            if ("NATURAL".equals(person.type()) && !blank(person.birthDate())) PersianDates.parse(person.birthDate());
            else if ("LEGAL".equals(person.type()) && !blank(person.registrationDate())) PersianDates.parse(person.registrationDate());
        } catch (IllegalArgumentException exception) {
            throw SanimXmlValues.invalid("اطلاعات درخواست نامعتبر است: " + exception.getMessage());
        }
        normalizePeriod(d);
        SanimModelValidation.requireAmounts(d.getAmounts());
    }

    public static void normalizePeriod(SanimDocumentDetails document) {
        try {
            int year = Integer.parseInt(PersianDates.digits(document.getTaxYear()));
            if (year > PersianDates.currentYear()) throw new IllegalArgumentException("سال مالیاتی نباید از سال جاری بزرگ‌تر باشد.");
            Integer period = blank(document.getTaxPeriod()) ? null : Integer.valueOf(PersianDates.digits(document.getTaxPeriod()));
            if (period != null) TaxPeriod.of(period);
            document.setTaxYear(Integer.toString(year));
            document.setTaxPeriod(period == null ? null : period.toString());
            if (blank(document.getTaxPeriodStartRaw())) document.setTaxPeriodStartRaw(PersianDates.periodStart(year, period));
            if (blank(document.getTaxPeriodEndRaw())) document.setTaxPeriodEndRaw(PersianDates.periodEnd(year, period));
            // Supplied boundaries are authoritative; do not replace them with calculated dates.
            var start = PersianDates.parse(document.getTaxPeriodStartRaw());
            var end = PersianDates.parse(document.getTaxPeriodEndRaw());
            if (end.isBefore(start)) throw new IllegalArgumentException("تاریخ پایان دوره نباید پیش از تاریخ شروع باشد.");
        } catch (RuntimeException exception) {
            throw SanimXmlValues.invalid("سال یا دوره مالیاتی نامعتبر است. " + (exception.getMessage() == null ? "" : exception.getMessage()));
        }
    }
    private static void required(List<String> missing, String value, String label) { if (blank(value)) missing.add(label); }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static void fail(List<String> missing) {
        if (!missing.isEmpty()) throw SanimXmlValues.invalid("درخواست رد شد؛ تکمیل این اطلاعات الزامی است: " + String.join("، ", missing));
    }
}
