package com.mparsict.vosolmaliat.application.dto.response;

import java.util.List;

public record ExecutionPaperRelatedPersonsResponse(
        Long executionPaperId,
        Long ownerLegalPersonId,
        String ownerNationalId,
        String debtYear,
        String lastSyncedAt,
        List<RelatedPersonResponse> relatedPersons
) {
}
