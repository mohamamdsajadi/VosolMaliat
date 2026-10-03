package com.mparsict.vosolmaliat.application.service;

import com.mparsict.vosolmaliat.application.dto.request.*;
import com.mparsict.vosolmaliat.domain.enums.PaperType;
import com.mparsict.vosolmaliat.domain.enums.TaxpayerType;
import com.mparsict.vosolmaliat.domain.model.sanim.*;
import com.mparsict.vosolmaliat.domain.repository.NaturalPersonRepository;
import com.mparsict.vosolmaliat.domain.repository.LegalPersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import static com.mparsict.vosolmaliat.application.service.SanimXmlValues.*;
import static com.mparsict.vosolmaliat.application.service.DebtPaperXmlField.*;

@Component
@RequiredArgsConstructor
public class DebtPaperPushMapper {
    private final DebtPaperXmlReader xmlReader;
    private final NaturalPersonRepository naturalPersons;
    private final LegalPersonRepository legalPersons;

    public DebtPaperIssuanceRequest map(DebtPaperPushRequest source) {
        if (source.announcementType() == null) throw invalid("announcementType is required.");
        PaperType paperType;
        try {
            paperType = PaperType.fromCode(source.announcementType());
        } catch (IllegalArgumentException exception) {
            throw invalid("Unsupported announcementType: " + source.announcementType());
        }
        DebtPaperMappingConfig config = DebtPaperMappingConfig.forPaperType(paperType);
        if (source.followCode() == null || source.followCode().isBlank()) throw invalid("Registration followCode is required.");
        var parsedXml = xmlReader.parse(source.xml());
        SanimXmlValues xml = new SanimXmlValues(parsedXml);
        SanimAmounts amounts = SanimAggregateMapper.map(paperType, xml);
        String identifier = digits(config.required(xml, IDENTITY_NUMBER));
        if (!identifier.equals(digits(source.nationalID().trim())))
            throw invalid("nationalID conflicts with XML identity.");
        TaxpayerType type =
                resolveType(config.text(xml, PERSON_TYPE), identifier);
        boolean foreign = type == TaxpayerType.NATURAL
                && naturalPersons.findByFida(identifier).isPresent();
        String name = config.required(xml, TAXPAYER_NAME);
        String year = digits(config.required(xml, TAX_YEAR));
        if (!DebtPaperIdentifierFormat.TAX_YEAR.matches(year)) throw invalid("Invalid XML tax year.");
        String period = config.taxPeriod(xml);
        if (source.taxOfficeId() == null || source.taxOfficeId().isBlank()) throw invalid("Sanim taxOfficeID is required.");
        // The legacy envelope taxAmount denotes the reported tax aggregate; never silently replace XML.
        if (source.taxAmount() != null && !source.taxAmount().isBlank()
                && decimal(source.taxAmount(), "taxAmount").compareTo(amounts.getTotalTaxAmount()) != 0)
            throw invalid("Envelope taxAmount differs from mapped totalTaxAmount; review required.");
        SanimDocumentDetails details = SanimDocumentDetails.builder()
                .paperType(paperType).paperName(paperType.getPersianTitle()).mappingVersion(SanimAggregateMapper.VERSION)
                .registrationTraceCode(source.followCode().trim()).xml(source.xml())
                .financialDetails(config.financialDetails(parsedXml, xml))
                .documentNumber(config.required(xml, DOCUMENT_NUMBER))
                .issueDateRaw(config.required(xml, ISSUE_DATE))
                .basisDocumentNumber(config.text(xml, BASIS_DOCUMENT_NUMBER))
                .basisDocumentDateRaw(config.text(xml, BASIS_DOCUMENT_DATE))
                .issuanceReason(config.issuanceReason(xml))
                .taxYear(year).taxPeriod(period)
                .taxPeriodStartRaw(config.text(xml, TAX_PERIOD_START))
                .taxPeriodEndRaw(config.text(xml, TAX_PERIOD_END))
                .taxpayerIdentifier(digits(config.required(xml, TAXPAYER_IDENTIFIER)))
                .taxpayerIdentifierType(config.taxpayerIdentifierType())
                .identityNumber(identifier)
                .identityType(identityType(type, foreign))
                .personType(type.name()).taxpayerName(name).tradeName(config.text(xml, TRADE_NAME))
                .activityTitle(config.text(xml, ACTIVITY_TITLE))
                .phone(config.text(xml, PHONE))
                .postalCode(config.text(xml, POSTAL_CODE))
                .address(config.text(xml, ADDRESS))
                .generalDirectorateName(config.text(xml, GENERAL_DIRECTORATE_NAME))
                .taxOfficeName(config.text(xml, TAX_OFFICE_NAME))
                .sourceTaxOfficeCode(source.taxOfficeId()).currencyCode(config.currencyCode())
                .taxAmountBasis(config.taxAmountBasis())
                .remissionBasis(config.remissionBasis())
                .validationStatus(config.validationStatus()).amounts(amounts).build();
        ModelRequestValidation.normalizePeriod(details);
        var taxpayer = new DebtPaperIssuanceRequest.TaxpayerInfo(type.name(),
                type == TaxpayerType.NATURAL && !foreign ? identifier : null,
                foreign ? identifier : null,
                type == TaxpayerType.NATURAL ? name : null,
                null, null, null, details.getPostalCode(), details.getAddress(),
                type == TaxpayerType.LEGAL ? identifier : null,
                null, null,
                type == TaxpayerType.LEGAL ? name : null);
        return new DebtPaperIssuanceRequest(config.debtPaperUniqueCode(source, details), source.taxSource(),
                null, null, taxpayer, null, details);
    }

    private TaxpayerType resolveType(String raw, String identifier) {
        boolean natural = naturalPersons.findByNationalCode(identifier).isPresent() || naturalPersons.findByFida(identifier).isPresent();
        boolean legal = legalPersons.findByNationalId(identifier).isPresent();
        TaxpayerType type = raw == null
                ? null
                : DebtPaperSourceTaxpayerType.fromValue(digits(raw)).taxpayerType();
        if (natural && legal || type != null
                && (type == TaxpayerType.NATURAL && legal
                || type == TaxpayerType.LEGAL && natural))
            throw invalid("Conflicting taxpayer identity/type requires review.");
        if (type == null) {
            if (natural && legal) throw invalid("Conflicting taxpayer identity/type requires review.");
            if (natural || legal) type = natural
                    ? TaxpayerType.NATURAL
                    : TaxpayerType.LEGAL;
            else if (DebtPaperIdentifierFormat.NATURAL_PERSON.matches(identifier))
                type = TaxpayerType.NATURAL;
            else if (DebtPaperIdentifierFormat.LEGAL_PERSON.matches(identifier))
                type = TaxpayerType.LEGAL;
            else throw invalid("Taxpayer type cannot be resolved; review required.");
        }
        if (type == TaxpayerType.NATURAL
                && !natural && !DebtPaperIdentifierFormat.NATURAL_PERSON.matches(identifier))
            throw invalid("Natural person identity requires review.");
        if (type == TaxpayerType.LEGAL
                && !legal && !DebtPaperIdentifierFormat.LEGAL_PERSON.matches(identifier))
            throw invalid("Legal person identity requires review.");
        return type;
    }

    private String identityType(TaxpayerType taxpayerType, boolean foreign) {
        if (foreign) return DebtPaperMappedValue.FIDA.value();
        return taxpayerType == TaxpayerType.LEGAL
                ? DebtPaperMappedValue.NATIONAL_ID.value()
                : DebtPaperMappedValue.NATIONAL_CODE.value();
    }
}
