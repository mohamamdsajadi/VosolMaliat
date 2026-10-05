package com.mparsict.vosolmaliat.unit.domain;
import com.mparsict.vosolmaliat.domain.value.PersianDates;
import com.mparsict.vosolmaliat.application.service.ModelRequestValidation;
import com.mparsict.vosolmaliat.application.dto.request.SanimDocumentDetails;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;
class PersianDatesTest {
    @Test void convertsNowruzAndPersianDigitsWithoutLosingTime() {
        assertThat(PersianDates.parse("۱۴۰۵/۰۱/۰۱T12:45:30")).isEqualTo(LocalDateTime.of(2026,3,21,12,45,30));
        assertThat(PersianDates.format(PersianDates.parse("1403/12/30"))).isEqualTo("1403/12/30");
        assertThatThrownBy(()->PersianDates.parse("1404/12/30")).hasMessageContaining("تاریخ شمسی نامعتبر");
    }
    @Test void computesMissingSeasonAndAnnualBoundariesButPreservesSuppliedDates() {
        var d=SanimDocumentDetails.builder().taxYear("1403").taxPeriod("4").build();
        ModelRequestValidation.normalizePeriod(d);
        assertThat(d.getTaxPeriodStartRaw()).isEqualTo("1403/10/01");
        assertThat(d.getTaxPeriodEndRaw()).isEqualTo("1403/12/30");
        d=SanimDocumentDetails.builder().taxYear("1404").taxPeriod("1")
                .taxPeriodStartRaw("1404/02/02").taxPeriodEndRaw("1404/04/01").build();
        ModelRequestValidation.normalizePeriod(d);
        assertThat(d.getTaxPeriodStartRaw()).isEqualTo("1404/02/02");
        assertThat(d.getTaxPeriodEndRaw()).isEqualTo("1404/04/01");
        d=SanimDocumentDetails.builder().taxYear("1404").build();
        ModelRequestValidation.normalizePeriod(d);
        assertThat(d.getTaxPeriodEndRaw()).isEqualTo("1404/12/29");
    }
    @Test void rejectsFutureYearsAndInvalidPeriods() {
        for (String period:new String[]{"0","5"}) {
            var d=SanimDocumentDetails.builder().taxYear("1404").taxPeriod(period).build();
            assertThatThrownBy(()->ModelRequestValidation.normalizePeriod(d)).hasMessageContaining("دوره مالیاتی");
        }
        var future=SanimDocumentDetails.builder().taxYear("1599").taxPeriod("1").build();
        assertThatThrownBy(()->ModelRequestValidation.normalizePeriod(future)).hasMessageContaining("سال جاری");
    }
}
