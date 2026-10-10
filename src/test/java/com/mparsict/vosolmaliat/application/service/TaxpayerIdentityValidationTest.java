package com.mparsict.vosolmaliat.application.service;

import com.mparsict.vosolmaliat.application.dto.request.*;
import com.mparsict.vosolmaliat.common.exception.BusinessException;
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

    @Test void conflictingRegistrationIsRejectedBeforeDatabaseConstraintFailure() {
        var repository = mock(TaxpayerRepository.class);
        when(repository.findByRegistrationTraceCode("2001")).thenReturn(Optional.of(
                Taxpayer.builder().tin("1002").registrationTraceCode("2001").build()));
        assertThatThrownBy(() -> TaxpayerIdentityValidation.requireConsistentRegistration(repository, request()))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo("FOLLOW_CODE_TIN_CONFLICT"));
    }

    @Test void existingRegistrationCanBeReusedForAnotherPaper() {
        var repository = mock(TaxpayerRepository.class);
        var taxpayer = Taxpayer.builder().tin("1001").registrationTraceCode("2001").build();
        when(repository.findByRegistrationTraceCode("2001")).thenReturn(Optional.of(taxpayer));
        when(repository.findByTin("1001")).thenReturn(Optional.of(taxpayer));
        assertThatCode(() -> TaxpayerIdentityValidation.requireConsistentRegistration(repository, request())).doesNotThrowAnyException();
    }

    @Test void personCannotReceiveASecondTinUnderTheCurrentUniqueConstraint() {
        var repository = mock(TaxpayerRepository.class);
        when(repository.findByNaturalPerson_NationalCode("0123456789")).thenReturn(Optional.of(
                Taxpayer.builder().tin("different-tin").registrationTraceCode("other-follow").build()));
        assertThatThrownBy(() -> TaxpayerIdentityValidation.requireConsistentRegistration(repository, request()))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo("PERSON_TIN_CONFLICT"));
    }
}
