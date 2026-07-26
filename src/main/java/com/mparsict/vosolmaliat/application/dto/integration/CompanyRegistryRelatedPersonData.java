package com.mparsict.vosolmaliat.application.dto.integration;

import com.mparsict.vosolmaliat.domain.enums.RelatedPersonPostCode;
import com.mparsict.vosolmaliat.domain.enums.RelatedPersonType;

public record CompanyRegistryRelatedPersonData(
        String sourceRecordId,
        RelatedPersonType personType,
        RelatedPersonPostCode postCode,
        String startDate,
        String endDate,
        boolean hasSignatoryAuthority,
        String nationalCode,
        String fida,
        String firstName,
        String lastName,
        String fatherName,
        String birthDate,
        String nationalId,
        String registrationNumber,
        String registrationDate,
        String companyName,
        CompanyRegistryNaturalPersonData legalRepresentative
) {
}
