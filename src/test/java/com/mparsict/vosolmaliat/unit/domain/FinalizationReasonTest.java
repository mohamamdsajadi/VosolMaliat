package com.mparsict.vosolmaliat.unit.domain;

import com.mparsict.vosolmaliat.domain.enums.FinalizationReason;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FinalizationReasonTest {
    @Test
    void acceptsDocumentLabelsSourceSpellingsAndPersistedCodes() {
        for (String value : new String[]{"برگه بدهی قطعی", "برگ قطعی", "برگه بدهي قطعي", "FINAL_NOTICE"}) {
            assertThat(FinalizationReason.fromSourceValue(value)).isEqualTo(FinalizationReason.FINAL_NOTICE);
        }
        for (String value : new String[]{"خوداظهاری", "خود اظهاری", "خود‌اظهاری", "SELF_DECLARATION"}) {
            assertThat(FinalizationReason.fromSourceValue(value)).isEqualTo(FinalizationReason.SELF_DECLARATION);
        }
        assertThat(FinalizationReason.FINAL_NOTICE.label()).isEqualTo("برگه بدهی قطعی");
    }

    @Test
    void rejectsMissingAndUnknownReasonsWithPersianMessage() {
        for (String value : new String[]{null, "", "UNKNOWN"}) {
            assertThatThrownBy(() -> FinalizationReason.fromSourceValue(value))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("علت قطعیت الزامی است");
        }
    }
}
