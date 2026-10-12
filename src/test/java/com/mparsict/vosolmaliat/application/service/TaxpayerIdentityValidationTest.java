package com.mparsict.vosolmaliat.application.service;

import com.mparsict.vosolmaliat.application.dto.request.*;
import com.mparsict.vosolmaliat.domain.model.Taxpayer;
import com.mparsict.vosolmaliat.domain.repository.TaxpayerRepository;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaxpayerIdentityValidationTest {
    DebtPaperIssuanceRequest request() {
        var person = new DebtPaperIssuanceRequest.TaxpayerInfo("NATURAL", "0123456789", null,
                "Test", null, null, null, null, null, null, null, null, null);
        return new DebtPaperIssuanceRequest("source", "TAXB", null, null, person, null,
                SanimDocumentDetails.builder().taxpayerIdentifier("1001").registrationTraceCode("2001").build());
    }

    @Test void registrationCodeMayBeSharedByDifferentNaturalTaxpayers() {
        var repository = mock(TaxpayerRepository.class);
        when(repository.findByRegistrationTraceCode("2001")).thenReturn(java.util.List.of(
                Taxpayer.builder().tin("1002").registrationTraceCode("2001").build()));
        assertThatCode(() -> TaxpayerIdentityValidation.requireConsistentRegistration(repository, request()))
                .doesNotThrowAnyException();
    }

    @Test void existingRegistrationCanBeReusedForAnotherPaper() {
        var repository = mock(TaxpayerRepository.class);
        var taxpayer = Taxpayer.builder().tin("1001").registrationTraceCode("2001").build();
        when(repository.findByRegistrationTraceCode("2001")).thenReturn(java.util.List.of(taxpayer));
        when(repository.findByTin("1001")).thenReturn(Optional.of(taxpayer));
        assertThatCode(() -> TaxpayerIdentityValidation.requireConsistentRegistration(repository, request())).doesNotThrowAnyException();
    }

    @Test void naturalPersonMayOwnMultipleTinValues() {
        var repository = mock(TaxpayerRepository.class);
        when(repository.findByNaturalPerson_NationalCode("0123456789")).thenReturn(java.util.List.of(
                Taxpayer.builder().tin("different-tin").registrationTraceCode("other-follow").build()));
        assertThatCode(() -> TaxpayerIdentityValidation.requireConsistentRegistration(repository, request()))
                .doesNotThrowAnyException();
    }
}
