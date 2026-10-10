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
        java.util.Optional<com.mparsict.vosolmaliat.domain.model.Taxpayer> owner;
        if ("LEGAL".equalsIgnoreCase(person.type())) {
            owner = taxpayers.findByLegalPerson_NationalId(person.nationalId());
        } else if (person.nationalCode() != null) {
            owner = taxpayers.findByNaturalPerson_NationalCode(person.nationalCode());
        } else {
            owner = taxpayers.findByNaturalPerson_Fida(person.fida());
        }
        owner.ifPresent(existing -> {
            if (!Objects.equals(tin, existing.getTin())) {
                throw new BusinessException("PERSON_TIN_CONFLICT", "در مدل فعلی، برای این شخص قبلاً شناسه مؤدی دیگری ثبت شده است");
            }
        });
        taxpayers.findByRegistrationTraceCode(follow).ifPresent(existing -> {
            if (!Objects.equals(tin, existing.getTin())) {
                throw new BusinessException("FOLLOW_CODE_TIN_CONFLICT", "کد رهگیری ثبت‌نام قبلاً برای شناسه مؤدی دیگری ثبت شده است");
            }
        });
        taxpayers.findByTin(tin).ifPresent(existing -> {
            if (!Objects.equals(follow, existing.getRegistrationTraceCode())) {
                throw new BusinessException("TIN_IDENTITY_CONFLICT", "شناسه مؤدی با کد رهگیری ثبت‌نام سازگار نیست");
            }
        });
    }
}
