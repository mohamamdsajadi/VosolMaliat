package com.mparsict.vosolmaliat.application.dto.request;
import com.mparsict.vosolmaliat.domain.enums.PaperType;
import com.mparsict.vosolmaliat.domain.model.sanim.DebtFinancialDetails;
import com.mparsict.vosolmaliat.domain.model.sanim.SanimAmounts;
import lombok.*;
/** Transport-only facts parsed from a SANIM payload; this class is not persisted as part of the domain schema. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SanimDocumentDetails {
    private Long activityTypeId;
    private String activityCode;
    private String mobile;
    private String registrationTraceCode;
    private String xml;
    private DebtFinancialDetails financialDetails;
    private PaperType paperType;
    private String paperName;
    private String mappingVersion;
    @jakarta.validation.constraints.NotBlank
    private String documentNumber;
    @jakarta.validation.constraints.NotBlank
    private String issueDateRaw;
    private String basisDocumentNumber;
    private String basisDocumentDateRaw;
    private String issuanceReason;
    private String taxYear;
    private String taxPeriod;
    private String taxPeriodStartRaw;
    private String taxPeriodEndRaw;
    private String taxpayerIdentifier;
    private String taxpayerIdentifierType;
    private String identityNumber;
    private String identityType;
    private String personType;
    private String taxpayerName;
    private String tradeName;
    private String activityTitle;
    private String phone;
    private String postalCode;
    private String address;
    private String generalDirectorateName;
    private String taxOfficeName;
    private String sourceTaxOfficeCode;
    private String currencyCode;
    private String taxAmountBasis;
    private String remissionBasis;
    private String validationStatus;
    @jakarta.validation.Valid
    @jakarta.validation.constraints.NotNull
    private SanimAmounts amounts;

    public void normalizePaperName() {
        paperName = paperType == null ? null : paperType.getPersianTitle();
    }
}
