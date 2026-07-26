package com.mparsict.vosolmaliat.application.dto.integration;

import com.mparsict.vosolmaliat.domain.enums.RelatedPersonRole;
import com.mparsict.vosolmaliat.domain.enums.RelatedPersonType;

public record CompanyRegistryRelatedPersonData(
        String sourceRecordId,
        RelatedPersonType personType,
        RelatedPersonRole role,
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
        String companyName
) {
}
