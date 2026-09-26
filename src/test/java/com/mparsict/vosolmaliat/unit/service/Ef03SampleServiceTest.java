package com.mparsict.vosolmaliat.unit.service;

import com.mparsict.vosolmaliat.application.service.DebtPaperXmlReader;
import com.mparsict.vosolmaliat.application.service.Ef03SampleService;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class Ef03SampleServiceTest {
    @Test
    void readsRealOdsAndBuildsContractFields() {
        var service = new Ef03SampleService(
                new DebtPaperXmlReader(),
                "docs/sample-execution-papers/EF03-samples.ods"
        );

        var samples = service.samples();

        assertThat(samples).hasSize(43);
        assertThat(samples).allSatisfy(sample -> {
            assertThat(sample.referenceID()).isNotBlank();
            assertThat(sample.taxOfficeID()).isNotBlank();
            assertThat(sample.taxAmount()).isNotBlank();
            assertThat(sample.xml()).startsWith("<content>");
        });
        var first = samples.getFirst();
        assertThat(first.announcementType()).isEqualTo("LTR_EF03");
        assertThat(first.referenceID()).isEqualTo("200000374736440");
        assertThat(first.nationalID()).isEqualTo("1270130269");
        assertThat(first.followCode()).isEqualTo("2557857342");
        assertThat(first.secondParameter()).isEqualTo("7126982199");
        assertThat(first.year()).isEqualTo(1403);
        assertThat(first.period()).isEqualTo(1);
        assertThat(first.taxOfficeID()).isEqualTo("2028");
        assertThat(first.taxAmount()).isEqualTo("2307314856");
        assertThat(first.taxSource()).isEqualTo("TAXB");
        assertThat(first.hash()).hasSize(64);
        assertThat(first.xml()).contains("اظهارنامه مالیات بر درآمد مشاغل گروه سوم");
    }
}
