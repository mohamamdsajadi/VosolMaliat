package com.mparsict.vosolmaliat.application.service;

import com.mparsict.vosolmaliat.application.dto.request.DebtPaperIssuanceRequest;
import com.mparsict.vosolmaliat.application.dto.request.DebtPaperPushRequest;
import com.mparsict.vosolmaliat.application.dto.request.SanimDocumentDetails;
import com.mparsict.vosolmaliat.domain.enums.FinalizationReason;
import com.mparsict.vosolmaliat.domain.enums.PaperType;
import com.mparsict.vosolmaliat.domain.enums.TaxpayerType;
import com.mparsict.vosolmaliat.domain.model.sanim.SanimAmounts;
import com.mparsict.vosolmaliat.domain.repository.LegalPersonRepository;
import com.mparsict.vosolmaliat.domain.repository.NaturalPersonRepository;
import com.mparsict.vosolmaliat.domain.value.PersianDates;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

import static com.mparsict.vosolmaliat.application.service.DebtPaperXmlField.*;

/** Maps spreadsheet imports without applying the validation contract of the online push API. */
@Component
@RequiredArgsConstructor
public class ExecutionPaperLenientImportMapper {
    private final DebtPaperXmlReader xmlReader;
    private final NaturalPersonRepository naturalPersons;
    private final LegalPersonRepository legalPersons;

    public DebtPaperIssuanceRequest map(DebtPaperPushRequest source) {
        PaperType paperType = PaperType.fromCode(source.announcementType());
        var document = xmlReader.parse(source.xml());
        var xml = new SanimXmlValues(document);
        var config = DebtPaperMappingConfig.forPaperType(paperType);

        String identity = digits(first(source.nationalID(), raw(config, xml, IDENTITY_NUMBER),
                numericFallback(source.hash())));
        if (identity.isBlank()) identity = numericFallback(source.hash());
        TaxpayerType type = taxpayerType(raw(config, xml, PERSON_TYPE), identity);
        boolean foreign = type != TaxpayerType.LEGAL
                && naturalPersons.findByFida(identity).isPresent();
        String name = value(raw(config, xml, TAXPAYER_NAME));
        String reference = first(source.referenceId(), raw(config, xml, DOCUMENT_NUMBER),
                paperType.name() + "-" + numericFallback(source.hash()));
        int year = year(raw(config, xml, TAX_YEAR), source.year());
        int period = period(raw(config, xml, TAX_PERIOD), source.period(), paperType);
        String basisNumber = first(raw(config, xml, BASIS_DOCUMENT_NUMBER), reference);
        String issueDate = date(raw(config, xml, ISSUE_DATE), year);
        String basisDate = date(raw(config, xml, BASIS_DOCUMENT_DATE), year);
        String generalName = first(raw(config, xml, GENERAL_DIRECTORATE_NAME), "اداره کل نامشخص");
        String officeName = first(raw(config, xml, TAX_OFFICE_NAME),
                source.taxOfficeId() == null || source.taxOfficeId().isBlank()
                        ? "اداره مالیاتی نامشخص" : "اداره مالیاتی " + source.taxOfficeId().trim());
        String tin = digits(first(raw(config, xml, TAXPAYER_IDENTIFIER), identity));
        if (tin.isBlank()) tin = identity;

        SanimAmounts amounts;
        try {
            amounts = SanimAggregateMapper.map(paperType, xml);
        } catch (RuntimeException ignored) {
            amounts = zeroAmounts();
        }

        var details = SanimDocumentDetails.builder()
                .paperType(paperType).paperName(paperType.getPersianTitle())
                .mappingVersion(SanimAggregateMapper.VERSION)
                .registrationTraceCode(value(source.followCode())).xml(source.xml())
                .financialDetails(config.financialDetails(document, xml))
                .documentNumber(reference).issueDateRaw(issueDate)
                .basisDocumentNumber(basisNumber).basisDocumentDateRaw(basisDate)
                .issuanceReason(reason(raw(config, xml, ISSUANCE_REASON)))
                .taxYear(Integer.toString(year)).taxPeriod(Integer.toString(period))
                .taxPeriodStartRaw(PersianDates.periodStart(year, period))
                .taxPeriodEndRaw(PersianDates.periodEnd(year, period))
                .taxpayerIdentifier(tin).taxpayerIdentifierType(config.taxpayerIdentifierType())
                .identityNumber(identity)
                .identityType(type == TaxpayerType.LEGAL ? DebtPaperMappedValue.NATIONAL_ID.value()
                        : foreign ? DebtPaperMappedValue.FIDA.value() : DebtPaperMappedValue.NATIONAL_CODE.value())
                .personType(type.name()).taxpayerName(name).tradeName(value(raw(config, xml, TRADE_NAME)))
                .activityTitle(value(raw(config, xml, ACTIVITY_TITLE)))
                .phone(value(raw(config, xml, PHONE))).postalCode(value(raw(config, xml, POSTAL_CODE)))
                .address(value(raw(config, xml, ADDRESS)))
                .generalDirectorateName(generalName).taxOfficeName(officeName)
                .sourceTaxOfficeCode(value(source.taxOfficeId())).currencyCode(config.currencyCode())
                .taxAmountBasis(config.taxAmountBasis()).remissionBasis(config.remissionBasis())
                .validationStatus("IMPORTED_WITHOUT_SOURCE_VALIDATION").amounts(amounts).build();

        String nationalCode = type != TaxpayerType.LEGAL && DebtPaperIdentifierFormat.NATURAL_PERSON.matches(identity)
                ? identity : null;
        String fida = type != TaxpayerType.LEGAL && nationalCode == null ? identity : null;
        var taxpayer = new DebtPaperIssuanceRequest.TaxpayerInfo(type.name(), nationalCode, fida,
                type == TaxpayerType.LEGAL ? null : name, null, null, null,
                details.getPostalCode(), details.getAddress(),
                type == TaxpayerType.LEGAL ? identity : null, null, null,
                type == TaxpayerType.LEGAL ? name : null);
        return new DebtPaperIssuanceRequest(basisNumber, paperType.taxSource().name(),
                null, null, taxpayer, null, details);
    }

    private String raw(DebtPaperMappingConfig config, SanimXmlValues xml, DebtPaperXmlField field) {
        try { return xml.text(config.tag(field).code()); }
        catch (RuntimeException ignored) { return null; }
    }

    private TaxpayerType taxpayerType(String raw, String identity) {
        if (naturalPersons.findByNationalCode(identity).isPresent()
                || naturalPersons.findByFida(identity).isPresent()) return TaxpayerType.NATURAL;
        if (legalPersons.findByNationalId(identity).isPresent()) return TaxpayerType.LEGAL;
        try { return DebtPaperSourceTaxpayerType.fromValue(raw).taxpayerType(); }
        catch (RuntimeException ignored) {
            return DebtPaperIdentifierFormat.LEGAL_PERSON.matches(identity)
                    ? TaxpayerType.LEGAL : TaxpayerType.NATURAL;
        }
    }

    private int year(String xmlYear, int envelopeYear) {
        String digits = digits(xmlYear);
        if (DebtPaperIdentifierFormat.TAX_YEAR.matches(digits)) {
            int parsed = Integer.parseInt(digits);
            if (parsed >= 1200 && parsed <= PersianDates.currentYear()) return parsed;
        }
        if (envelopeYear >= 1200 && envelopeYear <= PersianDates.currentYear()) return envelopeYear;
        return PersianDates.currentYear();
    }

    private int period(String xmlPeriod, int envelopePeriod, PaperType paperType) {
        if (paperType == PaperType.EF03) return 1;
        try {
            int parsed = Integer.parseInt(digits(xmlPeriod));
            if (parsed >= 1 && parsed <= 4) return parsed;
        } catch (RuntimeException ignored) { }
        return envelopePeriod >= 1 && envelopePeriod <= 4 ? envelopePeriod : 1;
    }

    private String date(String candidate, int year) {
        try {
            if (PersianDates.parse(candidate) != null) return candidate;
        } catch (RuntimeException ignored) { }
        return PersianDates.periodStart(year, 1);
    }

    private String reason(String candidate) {
        try { return FinalizationReason.fromSourceValue(candidate).name(); }
        catch (RuntimeException ignored) { return FinalizationReason.FINAL_NOTICE.name(); }
    }

    private SanimAmounts zeroAmounts() {
        return SanimAmounts.builder().outstandingDebtAmount(BigDecimal.ZERO)
                .totalTaxAmount(BigDecimal.ZERO).totalDutyAmount(BigDecimal.ZERO)
                .totalPenaltyAmount(BigDecimal.ZERO).totalPaidAmount(BigDecimal.ZERO)
                .totalCreditAmount(BigDecimal.ZERO).totalRemissionAmount(BigDecimal.ZERO).build();
    }

    private String numericFallback(String hash) {
        String digits = digits(hash);
        if (digits.length() >= 16) return digits.substring(0, 16);
        return Integer.toUnsignedString(value(hash).hashCode()) + "0000000000";
    }

    private String first(String... values) {
        for (String candidate : values) if (candidate != null && !candidate.isBlank()) return candidate.trim();
        return "";
    }

    private String value(String value) { return value == null || "--".equals(value.trim()) ? "" : value.trim(); }
    private String digits(String value) {
        String normalized = SanimXmlValues.digits(value(value));
        return normalized == null ? "" : normalized.replaceAll("[^0-9]", "");
    }
}
