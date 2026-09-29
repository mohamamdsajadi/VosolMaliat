package com.mparsict.vosolmaliat.application.service;

import com.mparsict.vosolmaliat.application.dto.request.DebtPaperPushRequest;
import com.mparsict.vosolmaliat.domain.enums.PaperType;
import com.mparsict.vosolmaliat.domain.model.sanim.DebtFinancialDetails;
import com.mparsict.vosolmaliat.domain.model.sanim.SanimDocumentDetails;
import org.w3c.dom.Document;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.mparsict.vosolmaliat.application.service.DebtPaperXmlField.*;
import static com.mparsict.vosolmaliat.application.service.SanimXmlTag.*;
import static java.util.Map.entry;

/**
 * Declarative XML configuration for every supported debt-paper type.
 *
 * <p>To change a source tag, edit the field map of the corresponding enum
 * constant. To add a paper type, add one configuration constant and its
 * fixed-value selections here.</p>
 */
public enum DebtPaperMappingConfig {
    EF03(
            PaperType.EF03,
            ef03Tags(),
            EnumSet.of(BASIS_DOCUMENT_NUMBER, BASIS_DOCUMENT_DATE, TAX_PERIOD_START, TAX_PERIOD_END),
            DebtPaperMappedValue.DEFAULT_TAX_PERIOD,
            DebtPaperMappedValue.SANIM_TAXPAYER_ID,
            DebtPaperMappedValue.GROSS_REPORTED,
            DebtPaperMappedValue.COMBINED_EXEMPTION_REMISSION,
            DebtPaperMappedValue.VALIDATED,
            IssuanceReasonMode.CANONICAL,
            FinancialDetailsMode.EF03,
            ReferenceSource.BASIS_DOCUMENT),
    EF23(
            PaperType.EF23,
            vatTags(),
            EnumSet.noneOf(DebtPaperXmlField.class),
            null,
            DebtPaperMappedValue.SANIM_TAXPAYER_ID,
            DebtPaperMappedValue.GROSS_REPORTED,
            DebtPaperMappedValue.NOT_REPORTED,
            DebtPaperMappedValue.VALIDATED_WITH_PROVISIONAL_MAPPING,
            IssuanceReasonMode.RAW,
            FinancialDetailsMode.NONE,
            ReferenceSource.ENVELOPE),
    EF23_1(
            PaperType.EF23_1,
            vatTags(),
            EnumSet.noneOf(DebtPaperXmlField.class),
            null,
            DebtPaperMappedValue.SANIM_TAXPAYER_ID,
            DebtPaperMappedValue.GROSS_REPORTED,
            DebtPaperMappedValue.NOT_REPORTED,
            DebtPaperMappedValue.VALIDATED_WITH_PROVISIONAL_MAPPING,
            IssuanceReasonMode.RAW,
            FinancialDetailsMode.NONE,
            ReferenceSource.ENVELOPE),
    EF23_2(
            PaperType.EF23_2,
            vatTags(),
            EnumSet.noneOf(DebtPaperXmlField.class),
            null,
            DebtPaperMappedValue.SANIM_TAXPAYER_ID,
            DebtPaperMappedValue.NET_REPORTED,
            DebtPaperMappedValue.NOT_REPORTED,
            DebtPaperMappedValue.VALIDATED_WITH_PROVISIONAL_MAPPING,
            IssuanceReasonMode.RAW,
            FinancialDetailsMode.NONE,
            ReferenceSource.ENVELOPE),
    EF104(
            PaperType.EF104,
            penaltyTags(),
            EnumSet.noneOf(DebtPaperXmlField.class),
            null,
            DebtPaperMappedValue.ECONOMIC_NUMBER,
            DebtPaperMappedValue.NOT_REPORTED,
            DebtPaperMappedValue.REPORTED,
            DebtPaperMappedValue.VALIDATED_WITH_PROVISIONAL_MAPPING,
            IssuanceReasonMode.NONE,
            FinancialDetailsMode.NONE,
            ReferenceSource.ENVELOPE);

    private final PaperType paperType;
    private final Map<DebtPaperXmlField, SanimXmlTag> tags;
    private final Set<DebtPaperXmlField> requiredMappedFields;
    private final DebtPaperMappedValue defaultTaxPeriod;
    private final DebtPaperMappedValue taxpayerIdentifierType;
    private final DebtPaperMappedValue taxAmountBasis;
    private final DebtPaperMappedValue remissionBasis;
    private final DebtPaperMappedValue validationStatus;
    private final IssuanceReasonMode issuanceReasonMode;
    private final FinancialDetailsMode financialDetailsMode;
    private final ReferenceSource referenceSource;

    DebtPaperMappingConfig(
            PaperType paperType,
            Map<DebtPaperXmlField, SanimXmlTag> tags,
            Set<DebtPaperXmlField> requiredMappedFields,
            DebtPaperMappedValue defaultTaxPeriod,
            DebtPaperMappedValue taxpayerIdentifierType,
            DebtPaperMappedValue taxAmountBasis,
            DebtPaperMappedValue remissionBasis,
            DebtPaperMappedValue validationStatus,
            IssuanceReasonMode issuanceReasonMode,
            FinancialDetailsMode financialDetailsMode,
            ReferenceSource referenceSource) {
        this.paperType = paperType;
        this.tags = Map.copyOf(tags);
        this.requiredMappedFields = Set.copyOf(requiredMappedFields);
        this.defaultTaxPeriod = defaultTaxPeriod;
        this.taxpayerIdentifierType = taxpayerIdentifierType;
        this.taxAmountBasis = taxAmountBasis;
        this.remissionBasis = remissionBasis;
        this.validationStatus = validationStatus;
        this.issuanceReasonMode = issuanceReasonMode;
        this.financialDetailsMode = financialDetailsMode;
        this.referenceSource = referenceSource;
    }

    public static DebtPaperMappingConfig forPaperType(PaperType paperType) {
        for (DebtPaperMappingConfig config : values()) {
            if (config.paperType == paperType) {
                return config;
            }
        }
        throw new IllegalArgumentException("No debt-paper mapping is configured for " + paperType);
    }

    public PaperType paperType() {
        return paperType;
    }

    public SanimXmlTag tag(DebtPaperXmlField field) {
        SanimXmlTag tag = tags.get(field);
        if (tag == null) {
            throw new IllegalArgumentException(
                    "No XML tag is configured for " + paperType + " field " + field);
        }
        return tag;
    }

    public String label(DebtPaperXmlField field) {
        return field.label();
    }

    String required(SanimXmlValues xml, DebtPaperXmlField field) {
        return xml.required(tag(field).code());
    }

    String text(SanimXmlValues xml, DebtPaperXmlField field) {
        SanimXmlTag tag = tags.get(field);
        if (tag == null) return null;
        return requiredMappedFields.contains(field)
                ? xml.required(tag.code())
                : xml.text(tag.code());
    }

    String taxPeriod(SanimXmlValues xml) {
        return defaultTaxPeriod == null
                ? SanimXmlValues.digits(required(xml, TAX_PERIOD))
                : defaultTaxPeriod.value();
    }

    String issuanceReason(SanimXmlValues xml) {
        return switch (issuanceReasonMode) {
            case CANONICAL -> DebtPaperIssuanceReason
                    .fromSourceValue(required(xml, ISSUANCE_REASON))
                    .code();
            case RAW -> text(xml, ISSUANCE_REASON);
            case NONE -> null;
        };
    }

    DebtFinancialDetails financialDetails(Document document, SanimXmlValues xml) {
        return switch (financialDetailsMode) {
            case EF03 -> Ef03FinancialDetailsMapper.map(document, xml);
            case NONE -> null;
        };
    }

    String debtPaperUniqueCode(DebtPaperPushRequest source, SanimDocumentDetails details) {
        return switch (referenceSource) {
            case BASIS_DOCUMENT -> details.getBasisDocumentNumber();
            case ENVELOPE -> source.referenceId();
        };
    }

    public String taxpayerIdentifierType() {
        return taxpayerIdentifierType.value();
    }

    public String currencyCode() {
        return DebtPaperMappedValue.IRR.value();
    }

    public String taxAmountBasis() {
        return taxAmountBasis.value();
    }

    public String remissionBasis() {
        return remissionBasis.value();
    }

    public String validationStatus() {
        return validationStatus.value();
    }

    private static Map<DebtPaperXmlField, SanimXmlTag> ef03Tags() {
        return tags(
                entry(DOCUMENT_NUMBER, F),
                entry(ISSUE_DATE, G),
                entry(BASIS_DOCUMENT_NUMBER, N),
                entry(BASIS_DOCUMENT_DATE, O),
                entry(ISSUANCE_REASON, M),
                entry(TAX_YEAR, C),
                entry(TAX_PERIOD_START, D),
                entry(TAX_PERIOD_END, E),
                entry(TAXPAYER_IDENTIFIER, Q),
                entry(PERSON_TYPE, P),
                entry(IDENTITY_NUMBER, R),
                entry(TAXPAYER_NAME, S),
                entry(TRADE_NAME, T),
                entry(ACTIVITY_TITLE, U),
                entry(PHONE, V),
                entry(POSTAL_CODE, W),
                entry(ADDRESS, X),
                entry(GENERAL_DIRECTORATE_NAME, H),
                entry(TAX_OFFICE_NAME, I));
    }

    private static Map<DebtPaperXmlField, SanimXmlTag> vatTags() {
        return tags(
                entry(DOCUMENT_NUMBER, D),
                entry(ISSUE_DATE, E),
                entry(BASIS_DOCUMENT_NUMBER, L),
                entry(BASIS_DOCUMENT_DATE, M),
                entry(ISSUANCE_REASON, K),
                entry(TAX_YEAR, B),
                entry(TAX_PERIOD, C),
                entry(TAXPAYER_IDENTIFIER, O),
                entry(PERSON_TYPE, N),
                entry(IDENTITY_NUMBER, P),
                entry(TAXPAYER_NAME, Q),
                entry(TRADE_NAME, R),
                entry(ACTIVITY_TITLE, S),
                entry(PHONE, T),
                entry(POSTAL_CODE, U),
                entry(ADDRESS, V),
                entry(GENERAL_DIRECTORATE_NAME, F),
                entry(TAX_OFFICE_NAME, G));
    }

    private static Map<DebtPaperXmlField, SanimXmlTag> penaltyTags() {
        return tags(
                entry(DOCUMENT_NUMBER, F),
                entry(ISSUE_DATE, G),
                entry(BASIS_DOCUMENT_NUMBER, O),
                entry(BASIS_DOCUMENT_DATE, P),
                entry(TAX_YEAR, B),
                entry(TAX_PERIOD, C),
                entry(TAXPAYER_IDENTIFIER, J),
                entry(IDENTITY_NUMBER, K),
                entry(TAXPAYER_NAME, H),
                entry(TRADE_NAME, I),
                entry(PHONE, M),
                entry(POSTAL_CODE, N),
                entry(ADDRESS, L),
                entry(GENERAL_DIRECTORATE_NAME, D),
                entry(TAX_OFFICE_NAME, E));
    }

    @SafeVarargs
    private static Map<DebtPaperXmlField, SanimXmlTag> tags(
            Map.Entry<DebtPaperXmlField, SanimXmlTag>... mappings) {
        EnumMap<DebtPaperXmlField, SanimXmlTag> result =
                new EnumMap<>(DebtPaperXmlField.class);
        for (Map.Entry<DebtPaperXmlField, SanimXmlTag> mapping : mappings) {
            if (result.put(mapping.getKey(), mapping.getValue()) != null) {
                throw new IllegalArgumentException(
                        "Duplicate XML mapping for field " + mapping.getKey());
            }
        }
        return result;
    }

    private enum IssuanceReasonMode {
        CANONICAL, RAW, NONE
    }

    private enum FinancialDetailsMode {
        EF03, NONE
    }

    private enum ReferenceSource {
        BASIS_DOCUMENT, ENVELOPE
    }
}
