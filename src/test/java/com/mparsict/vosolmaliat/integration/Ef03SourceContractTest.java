package com.mparsict.vosolmaliat.integration;

import com.mparsict.vosolmaliat.application.service.DebtPaperXmlReader;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class Ef03SourceContractTest {
    @Test void allEnvelopesPreserveTheOriginalXmlIncludingMissingFields() throws Exception {
        var fixture = new Ef03PipelineLiveTest();
        fixture.xmlReader = new DebtPaperXmlReader();
        var sources = fixture.readSource();
        assertThat(sources).hasSize(1000);
        for (var source : sources) {
            var envelope = fixture.envelope(source);
            assertThat(envelope.get("xml")).isEqualTo(source.columns().get("XML"));
            assertThat(envelope.get("referenceID")).isEqualTo(source.columns().get("RefrenceID").trim());
            assertThat(envelope.get("taxOfficeID")).asString().isNotBlank();
        }
        assertThat(sources.stream().filter(s -> !s.tags().containsKey("n")).count()).isEqualTo(1);
        assertThat(sources.stream().filter(s -> !s.tags().containsKey("w")).count()).isEqualTo(2);
    }
}
