package com.mparsict.vosolmaliat.unit.domain;

import com.mparsict.vosolmaliat.domain.enums.ExecutionPaperState;
import com.mparsict.vosolmaliat.domain.enums.TaxSourceType;
import com.mparsict.vosolmaliat.domain.enums.TaxpayerType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DomainLabelUnitTest {

    @Test
    void executionPaperStateReturnsPersianLabelAndKeepsUnknownCode() {
        assertThat(ExecutionPaperState.labelOf("INFORMED")).isEqualTo("ابلاغ شده");
        assertThat(ExecutionPaperState.labelOf("UNKNOWN_STATE")).isEqualTo("UNKNOWN_STATE");
        assertThat(ExecutionPaperState.labelOf(null)).isNull();
    }

    @Test
    void taxSourceDisplayReturnsPersianLabelWithEnglishCode() {
        assertThat(TaxSourceType.displayOf("VAT")).isEqualTo("مالیات بر ارزش افزوده (VAT)");
        assertThat(TaxSourceType.displayOf("CUSTOM_CODE")).isEqualTo("CUSTOM_CODE");
        assertThat(TaxSourceType.fromSourceValue("TAXB")).isEqualTo(TaxSourceType.INCOME_TAX);
    }

    @Test
    void taxpayerTypeLabelIsPersianOnly() {
        assertThat(TaxpayerType.labelOf("NATURAL")).isEqualTo("حقیقی");
        assertThat(TaxpayerType.labelOf("LEGAL")).isEqualTo("حقوقی");
    }
}
