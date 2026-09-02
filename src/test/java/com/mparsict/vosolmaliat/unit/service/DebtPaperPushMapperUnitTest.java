package com.mparsict.vosolmaliat.unit.service;

import com.mparsict.vosolmaliat.application.dto.request.DebtPaperPushRequest;
import com.mparsict.vosolmaliat.application.service.DebtPaperPushMapper;
import com.mparsict.vosolmaliat.application.service.DebtPaperXmlReader;
import com.mparsict.vosolmaliat.common.exception.DebtPaperPushException;
import com.mparsict.vosolmaliat.domain.model.masterdata.TaxAffairsOffice;
import com.mparsict.vosolmaliat.domain.model.masterdata.TaxUnit;
import com.mparsict.vosolmaliat.domain.repository.TaxAffairsOfficeRepository;
import com.mparsict.vosolmaliat.domain.repository.TaxUnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DebtPaperPushMapperUnitTest {

    private TaxAffairsOfficeRepository officeRepository;
    private TaxUnitRepository unitRepository;
    private DebtPaperPushMapper mapper;

    @BeforeEach
    void setUp() {
        officeRepository = mock(TaxAffairsOfficeRepository.class);
        unitRepository = mock(TaxUnitRepository.class);
        mapper = new DebtPaperPushMapper(new DebtPaperXmlReader(), officeRepository, unitRepository);

        TaxUnit unit = TaxUnit.builder()
                .id(7L)
                .taxAffairsOffice(TaxAffairsOffice.builder().id(3L).build())
                .build();
        when(officeRepository.existsById(3L)).thenReturn(true);
        when(unitRepository.findById(7L)).thenReturn(Optional.of(unit));
        when(unitRepository.findGeneralDirectorateIdByTaxUnitId(7L)).thenReturn(Optional.of(1L));
    }

    @Test
    void mapsOuterContractAndKnownXmlFieldsToExistingIssuanceRequest() {
        var mapped = mapper.map(request("""
                <DebtPaper>
                  <taxpayer>
                    <type>NATURAL</type>
                    <nationalCode>1234567890</nationalCode>
                    <firstName>Ali</firstName>
                    <lastName>Ahmadi</lastName>
                  </taxpayer>
                  <taxUnitId>7</taxUnitId>
                  <finalizationReason>FINAL</finalizationReason>
                </DebtPaper>
                """));

        assertThat(mapped.debtPaperUniqueCode()).isEqualTo("ref-100");
        assertThat(mapped.taxSource()).isEqualTo("TAXB");
        assertThat(mapped.performanceYear()).isEqualTo("1400");
        assertThat(mapped.performancePeriod()).isEqualTo("2");
        assertThat(mapped.taxBureauId()).isEqualTo(3L);
        assertThat(mapped.taxUnitId()).isEqualTo(7L);
        assertThat(mapped.taxpayer().type()).isEqualTo("NATURAL");
        assertThat(mapped.taxpayer().nationalCode()).isEqualTo("1234567890");
        assertThat(mapped.taxpayer().firstName()).isEqualTo("Ali");
        assertThat(mapped.debtInfo().taxAmount()).isEqualTo(1250.5);
    }

    @Test
    void rejectsXmlTaxUnitOutsideSuppliedTaxOffice() {
        TaxUnit mismatched = TaxUnit.builder()
                .id(7L)
                .taxAffairsOffice(TaxAffairsOffice.builder().id(9L).build())
                .build();
        when(unitRepository.findById(7L)).thenReturn(Optional.of(mismatched));

        assertThatThrownBy(() -> mapper.map(request("""
                <DebtPaper><taxpayer><type>NATURAL</type></taxpayer><taxUnitId>7</taxUnitId></DebtPaper>
                """)))
                .isInstanceOf(DebtPaperPushException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void rejectsDoctypeAndExternalEntityXml() {
        assertThatThrownBy(() -> mapper.map(request("""
                <!DOCTYPE foo [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <DebtPaper><taxpayer><type>NATURAL</type></taxpayer><taxUnitId>&xxe;</taxUnitId></DebtPaper>
                """)))
                .isInstanceOf(DebtPaperPushException.class)
                .hasMessageContaining("not well-formed");
    }

    private DebtPaperPushRequest request(String xml) {
        return new DebtPaperPushRequest(
                "1234567890",
                xml,
                "hash",
                OffsetDateTime.parse("2023-05-17T11:33:45.123Z"),
                "ref-100",
                "follow",
                "284",
                1400,
                2,
                3L,
                "1250.5",
                "TAXB"
        );
    }
}
