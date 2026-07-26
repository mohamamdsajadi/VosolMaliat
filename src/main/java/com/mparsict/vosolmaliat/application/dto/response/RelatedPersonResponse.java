package com.mparsict.vosolmaliat.application.dto.response;

public record RelatedPersonResponse(
        Long relationshipId,
        String personType,
        Long postId,
        String postCode,
        String postLabel,
        String startDate,
        String endDate,
        boolean hasSignatoryAuthority,
        Long naturalPersonId,
        String nationalCode,
        String fida,
        String firstName,
        String lastName,
        String fatherName,
        String birthDate,
        Long legalPersonId,
        String nationalId,
        String registrationNumber,
        String registrationDate,
        String companyName
) {
}
