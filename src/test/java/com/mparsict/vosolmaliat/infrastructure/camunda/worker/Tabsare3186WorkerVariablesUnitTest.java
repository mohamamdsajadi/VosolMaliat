package com.mparsict.vosolmaliat.infrastructure.camunda.worker;

import com.mparsict.vosolmaliat.application.dto.integration.CompanyRegistryRelatedPersonData;
import com.mparsict.vosolmaliat.domain.enums.RelatedPersonPostCode;
import com.mparsict.vosolmaliat.domain.enums.RelatedPersonType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Tabsare3186WorkerVariablesUnitTest {

    @Test
    void filtersAgainstExactJalaliDebtPaperDate() {
        CompanyRegistryRelatedPersonData endedBeforeDebtPaper = person(
                "OLD",
                "1400-01-01",
                "1402-12-29"
        );
        CompanyRegistryRelatedPersonData activeOnDebtPaper = person(
                "ACTIVE",
                "1402-12-29",
                "1403-01-10"
        );

        assertThat(Tabsare3186WorkerVariables.activeAt(
                endedBeforeDebtPaper,
                "۱۴۰۳/۰۱/۰۱"
        )).isFalse();
        assertThat(Tabsare3186WorkerVariables.activeAt(
                activeOnDebtPaper,
                "۱۴۰۳/۰۱/۰۱"
        )).isTrue();
    }

    private CompanyRegistryRelatedPersonData person(String id, String startDate, String endDate) {
        return new CompanyRegistryRelatedPersonData(
                id,
                RelatedPersonType.NATURAL,
                RelatedPersonPostCode.BOARD_MEMBER,
                startDate,
                endDate,
                false,
                "0012345678",
                null,
                "علی",
                "احمدی",
                "محمد",
                "1360-01-01",
                null,
                null,
                null,
                null,
                null
        );
    }
}
