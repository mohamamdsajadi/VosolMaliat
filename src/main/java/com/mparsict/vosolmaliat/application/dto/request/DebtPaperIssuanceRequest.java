package com.mparsict.vosolmaliat.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import com.mparsict.vosolmaliat.domain.model.sanim.SanimDocumentDetails;

/** One source document and its initial aggregates; organization/person relations remain separate. */
public record DebtPaperIssuanceRequest(
        @NotBlank String debtPaperUniqueCode,
        @NotBlank String taxSource,
        Long mainTaxBureauId,
        Long taxBureauId,
        Long taxUnitId,
        Long sendingUnitId,
        @NotNull @Valid TaxpayerInfo taxpayer,
        List<RelatedPersonInfo> relatedPersons,
        @NotNull @Valid SanimDocumentDetails sourceDocument
) {
    public String debtPaperIssueDate() {
        return sourceDocument == null ? null : sourceDocument.getBasisDocumentDateRaw();
    }

    public record TaxpayerInfo(
            @NotBlank String type,
            // Natural person fields
            String nationalCode,
            String fida,
            String firstName,
            String lastName,
            String fatherName,
            String birthDate,
            String postalCode,
            String address,
            // Legal person fields
            String nationalId,
            String registrationNumber,
            String registrationDate,
            String companyName
    ) {}


    public record RelatedPersonInfo(
            String type,
            String nationalCode,
            String nationalId,
            String postName,
            String startDate,
            String endDate
    ) {}
}
