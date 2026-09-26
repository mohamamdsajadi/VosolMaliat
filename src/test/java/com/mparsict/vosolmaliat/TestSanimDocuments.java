package com.mparsict.vosolmaliat;
import com.mparsict.vosolmaliat.domain.model.sanim.*;
import java.math.BigDecimal;
public final class TestSanimDocuments {
    public static SanimAmounts amounts() {
        return new SanimAmounts(new BigDecimal("1185"), new BigDecimal("1000"), new BigDecimal("200"),
                new BigDecimal("85"), new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO);
    }
    public static SanimDocumentDetails document(String number, String date, String year, String period, String activity) {
        return SanimDocumentDetails.builder().documentNumber(number).issueDateRaw(date).basisDocumentNumber(number).basisDocumentDateRaw(date)
                .taxYear(year).taxPeriod(period).activityTitle(activity).amounts(amounts()).build();
    }
}
