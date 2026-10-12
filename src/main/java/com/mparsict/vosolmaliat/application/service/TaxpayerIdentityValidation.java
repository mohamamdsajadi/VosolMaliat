package com.mparsict.vosolmaliat.application.service;

import com.mparsict.vosolmaliat.application.dto.request.DebtPaperIssuanceRequest;
import com.mparsict.vosolmaliat.common.exception.BusinessException;
import com.mparsict.vosolmaliat.domain.repository.TaxpayerRepository;
import java.util.Objects;

/** Reject known registration conflicts before a process is started; do not rewrite source identities. */
final class TaxpayerIdentityValidation {
    private TaxpayerIdentityValidation() {}

    static void requireConsistentRegistration(TaxpayerRepository taxpayers, DebtPaperIssuanceRequest request) {
        String tin = request.sourceDocument().getTaxpayerIdentifier();
        String follow = request.sourceDocument().getRegistrationTraceCode();
        var person = request.taxpayer();
        if ("LEGAL".equalsIgnoreCase(person.type())) {
            taxpayers.findByLegalPerson_NationalId(person.nationalId()).ifPresent(existing -> {
                if (!Objects.equals(tin, existing.getTin())) {
                    throw new BusinessException("PERSON_TIN_CONFLICT", "برای شخص حقوقی قبلاً شناسه مؤدی دیگری ثبت شده است");
                }
            });
        }
        taxpayers.findByTin(tin).ifPresent(existing -> {
            if (!Objects.equals(follow, existing.getRegistrationTraceCode())) {
                throw new BusinessException("TIN_IDENTITY_CONFLICT", "شناسه مؤدی با کد رهگیری ثبت‌نام سازگار نیست");
            }
        });
    }
}
