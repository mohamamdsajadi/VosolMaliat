package com.mparsict.vosolmaliat.domain.model.sanim;
import jakarta.persistence.*;
import lombok.*;
/** Immutable-in-use facts as printed on the source document, independent of mutable master data. */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SanimDocumentDetails {
    @Column(name="registration_trace_code") private String registrationTraceCode;
    @Column(name="source_xml", columnDefinition="TEXT") private String xml;
    @Embedded private DebtFinancialDetails financialDetails;
    @Column(name = "source_template_code", columnDefinition = "TEXT")
    private String templateCode;
    @Column(name = "source_mapping_version", columnDefinition = "TEXT")
    private String mappingVersion;
    @Column(name = "source_document_number", columnDefinition = "TEXT")
    @jakarta.validation.constraints.NotBlank
    private String documentNumber;
    @Column(name = "source_issue_date_raw", columnDefinition = "TEXT")
    @jakarta.validation.constraints.NotBlank
    private String issueDateRaw;
    @Column(name = "source_basis_document_number", columnDefinition = "TEXT")
    private String basisDocumentNumber;
    @Column(name = "source_basis_document_date_raw", columnDefinition = "TEXT")
    private String basisDocumentDateRaw;
    @Column(name = "source_issuance_reason", columnDefinition = "TEXT")
    private String issuanceReason;
    @Column(name = "source_tax_year", columnDefinition = "TEXT")
    private String taxYear;
    @Column(name = "source_tax_period", columnDefinition = "TEXT")
    private String taxPeriod;
    @Column(name = "source_tax_period_start_raw", columnDefinition = "TEXT")
    private String taxPeriodStartRaw;
    @Column(name = "source_tax_period_end_raw", columnDefinition = "TEXT")
    private String taxPeriodEndRaw;
    @Column(name = "source_taxpayer_identifier", columnDefinition = "TEXT")
    private String taxpayerIdentifier;
    @Column(name = "source_taxpayer_identifier_type", columnDefinition = "TEXT")
    private String taxpayerIdentifierType;
    @Column(name = "source_identity_number", columnDefinition = "TEXT")
    private String identityNumber;
    @Column(name = "source_identity_type", columnDefinition = "TEXT")
    private String identityType;
    @Column(name = "source_person_type", columnDefinition = "TEXT")
    private String personType;
    @Column(name = "source_taxpayer_name", columnDefinition = "TEXT")
    private String taxpayerName;
    @Column(name = "source_trade_name", columnDefinition = "TEXT")
    private String tradeName;
    @Column(name = "source_activity_title", columnDefinition = "TEXT")
    private String activityTitle;
    @Column(name = "source_phone", columnDefinition = "TEXT")
    private String phone;
    @Column(name = "source_postal_code", columnDefinition = "TEXT")
    private String postalCode;
    @Column(name = "source_address", columnDefinition = "TEXT")
    private String address;
    @Column(name = "source_general_directorate_name", columnDefinition = "TEXT")
    private String generalDirectorateName;
    @Column(name = "source_tax_office_name", columnDefinition = "TEXT")
    private String taxOfficeName;
    @Column(name = "source_tax_office_code", columnDefinition = "TEXT")
    private String sourceTaxOfficeCode;
    @Column(name = "source_currency_code", columnDefinition = "TEXT")
    private String currencyCode;
    @Column(name = "source_tax_amount_basis", columnDefinition = "TEXT")
    private String taxAmountBasis;
    @Column(name = "source_remission_basis", columnDefinition = "TEXT")
    private String remissionBasis;
    @Column(name = "source_validation_status", columnDefinition = "TEXT")
    private String validationStatus;
    @Embedded
    @jakarta.validation.Valid
    @jakarta.validation.constraints.NotNull
    private SanimAmounts amounts;
}
