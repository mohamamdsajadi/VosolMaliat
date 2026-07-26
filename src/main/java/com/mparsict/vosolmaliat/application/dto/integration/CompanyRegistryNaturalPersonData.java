package com.mparsict.vosolmaliat.application.dto.integration;

public record CompanyRegistryNaturalPersonData(
        String nationalCode,
        String fida,
        String firstName,
        String lastName,
        String fatherName,
        String birthDate
) {
}
