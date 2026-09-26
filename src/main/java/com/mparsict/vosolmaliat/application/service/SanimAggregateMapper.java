package com.mparsict.vosolmaliat.application.service;

import com.mparsict.vosolmaliat.domain.model.sanim.SanimAmounts;
import java.math.BigDecimal;

final class SanimAggregateMapper {
    static final String VERSION = "sanim-ef03-2026-09-v2";
    private static SanimAmounts ef03(SanimXmlValues x) {
        if (x.amount("GB").compareTo(x.amount("NB")) != 0)
            throw SanimXmlValues.invalid("DATA_INCONSISTENCY: ناسازگاری داده؛ مجموع جرائم GB با NB برابر نیست.");
        for (String tag : new String[]{"CB", "DB", "EB", "GB", "NB", "HB"})
            if (x.amount(tag).signum() < 0) throw SanimXmlValues.invalid("Negative EF03 amount: " + tag);
        BigDecimal calculatedDebt = x.amount("CB").add(x.amount("GB")).subtract(x.amount("DB")).subtract(x.amount("EB"));
        if (calculatedDebt.compareTo(x.amount("HB")) != 0)
            throw SanimXmlValues.invalid("DATA_INCONSISTENCY: مانده HB با مالیات + جرائم - بخشودگی/معافیت - پرداختی برابر نیست.");
        return new SanimAmounts(x.amount("HB"), x.amount("CB"), BigDecimal.ZERO,
                x.amount("GB"), x.amount("EB"), BigDecimal.ZERO, x.amount("DB"));
    }
    static SanimAmounts map(String template, SanimXmlValues x) {
        BigDecimal zero = BigDecimal.ZERO;
        return switch (template) {
            case "LTR_EF03" -> ef03(x);
            case "LTR_EF104" -> new SanimAmounts(x.amount("RB"), zero, zero,
                    x.amount("PB"), x.amount("XB"), zero, x.amount("QB"));
            case "LTR_EF23", "LTR_EF23_1" -> new SanimAmounts(
                    x.sum("LC", "MC", "BD", "CD", "JD").add(x.wasteAmount("SD")),
                    x.sum("NB", "RC"), x.sum("OB", "SC", "ED").add(x.wasteAmount("QD")),
                    x.sum("DC", "EC", "FC", "GC", "TC", "UC", "VC", "WC", "FD", "GD", "HD"),
                    x.sum("JC", "KC", "ZC", "AD", "ID").add(x.wasteAmount("RD")),
                    x.sum("XB", "YB", "BC", "CC"), zero);
            case "LTR_EF23_2" -> new SanimAmounts(x.sum("PE", "BD", "CD", "JD"),
                    x.sum("GE", "RC"), x.sum("SC", "ED"),
                    x.sum("IE", "JE", "KE", "LE", "ME", "NE", "TC", "UC", "VC", "WC", "FD", "GD", "HD"),
                    x.sum("OE", "ZC", "AD", "ID"), zero, zero);
            default -> throw SanimXmlValues.invalid("Unsupported announcementType: " + template);
        };
    }
}
