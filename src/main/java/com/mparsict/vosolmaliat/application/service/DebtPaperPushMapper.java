package com.mparsict.vosolmaliat.application.service;

import com.mparsict.vosolmaliat.application.dto.request.*;
import com.mparsict.vosolmaliat.domain.model.sanim.*;
import com.mparsict.vosolmaliat.domain.repository.NaturalPersonRepository;
import com.mparsict.vosolmaliat.domain.repository.LegalPersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Locale;
import static com.mparsict.vosolmaliat.application.service.SanimXmlValues.*;

@Component
@RequiredArgsConstructor
public class DebtPaperPushMapper {
    private final DebtPaperXmlReader xmlReader;
    private final NaturalPersonRepository naturalPersons;
    private final LegalPersonRepository legalPersons;

    public DebtPaperIssuanceRequest map(DebtPaperPushRequest source) {
        if (source.announcementType() == null) throw invalid("announcementType is required.");
        String template = source.announcementType().trim().toUpperCase(Locale.ROOT);
        if (template.equals("EF03")) template = "LTR_EF03";
        if (source.followCode() == null || source.followCode().isBlank()) throw invalid("Registration followCode is required.");
        var parsedXml = xmlReader.parse(source.xml());
        SanimXmlValues xml = new SanimXmlValues(parsedXml);
        SanimAmounts amounts = SanimAggregateMapper.map(template, xml);
        boolean direct = template.equals("LTR_EF03");
        boolean penalty = template.equals("LTR_EF104");
        String identifier = digits(xml.required(direct ? "R" : penalty ? "K" : "P"));
        if (!identifier.equals(digits(source.nationalID().trim())))
            throw invalid("nationalID conflicts with XML identity.");
        String type = resolveType(penalty ? null : xml.text(direct ? "P" : "N"), identifier);
        boolean foreign = type.equals("NATURAL") && naturalPersons.findByFida(identifier).isPresent();
        String name = xml.required(direct ? "S" : penalty ? "H" : "Q");
        String year = digits(xml.required(direct ? "C" : "B"));
        if (!year.matches("[0-9]{4}")) throw invalid("Invalid XML tax year.");
        String period = direct ? "1" : digits(xml.required("C"));
        String reason = direct ? xml.required("M").replace('ي','ی').replace(" ", "").replace("‌", "") : null;
        if (direct && !reason.equals("خوداظهاری") && !reason.equals("برگقطعی"))
            throw invalid("Invalid EF03 finalization reason.");
        if (source.taxOfficeId() == null || source.taxOfficeId().isBlank()) throw invalid("Sanim taxOfficeID is required.");
        // The legacy envelope taxAmount denotes the reported tax aggregate; never silently replace XML.
        if (source.taxAmount() != null && !source.taxAmount().isBlank()
                && decimal(source.taxAmount(), "taxAmount").compareTo(amounts.getTotalTaxAmount()) != 0)
            throw invalid("Envelope taxAmount differs from mapped totalTaxAmount; review required.");
        SanimDocumentDetails details = SanimDocumentDetails.builder()
                .templateCode(template).mappingVersion(SanimAggregateMapper.VERSION)
                .registrationTraceCode(source.followCode().trim()).xml(source.xml())
                .financialDetails(direct ? Ef03FinancialDetailsMapper.map(parsedXml, xml) : null)
                .documentNumber(xml.required(direct ? "F" : penalty ? "F" : "D"))
                .issueDateRaw(xml.required(direct ? "G" : penalty ? "G" : "E"))
                .basisDocumentNumber(direct ? xml.required("N") : xml.text(penalty ? "O" : "L"))
                .basisDocumentDateRaw(direct ? xml.required("O") : xml.text(penalty ? "P" : "M"))
                .issuanceReason(direct ? (reason.equals("خوداظهاری") ? "SELF_DECLARATION" : "FINAL_NOTICE") : penalty ? null : xml.text("K"))
                .taxYear(year).taxPeriod(period)
                .taxPeriodStartRaw(direct ? xml.required("D") : null).taxPeriodEndRaw(direct ? xml.required("E") : null)
                .taxpayerIdentifier(digits(xml.required(direct ? "Q" : penalty ? "J" : "O")))
                .taxpayerIdentifierType(penalty ? "ECONOMIC_NUMBER" : "SANIM_TAXPAYER_ID")
                .identityNumber(identifier).identityType(foreign ? "FIDA" : type.equals("LEGAL") ? "NATIONAL_ID" : "NATIONAL_CODE")
                .personType(type).taxpayerName(name).tradeName(xml.text(direct ? "T" : penalty ? "I" : "R"))
                .activityTitle(penalty ? null : xml.text(direct ? "U" : "S"))
                .phone(xml.text(direct ? "V" : penalty ? "M" : "T"))
                .postalCode(xml.text(direct ? "W" : penalty ? "N" : "U"))
                .address(xml.text(direct ? "X" : penalty ? "L" : "V"))
                .generalDirectorateName(xml.text(direct ? "H" : penalty ? "D" : "F"))
                .taxOfficeName(xml.text(direct ? "I" : penalty ? "E" : "G"))
                .sourceTaxOfficeCode(source.taxOfficeId()).currencyCode("IRR")
                .taxAmountBasis(template.equals("LTR_EF23_2") ? "NET_REPORTED" : penalty ? "NOT_REPORTED" : "GROSS_REPORTED")
                .remissionBasis(direct ? "COMBINED_EXEMPTION_REMISSION" : penalty ? "REPORTED" : "NOT_REPORTED")
                .validationStatus(direct ? "VALIDATED" : "VALIDATED_WITH_PROVISIONAL_MAPPING").amounts(amounts).build();
        var taxpayer = new DebtPaperIssuanceRequest.TaxpayerInfo(type,
                type.equals("NATURAL") && !foreign ? identifier : null, foreign ? identifier : null,
                type.equals("NATURAL") ? name : null, null, null, null, details.getPostalCode(), details.getAddress(),
                type.equals("LEGAL") ? identifier : null, null, null, type.equals("LEGAL") ? name : null);
        return new DebtPaperIssuanceRequest(direct ? details.getBasisDocumentNumber() : source.referenceId(), source.taxSource(),
                null, null, null, null, taxpayer, null, details);
    }

    private String resolveType(String raw, String identifier) {
        boolean natural = naturalPersons.findByNationalCode(identifier).isPresent() || naturalPersons.findByFida(identifier).isPresent();
        boolean legal = legalPersons.findByNationalId(identifier).isPresent();
        String type = raw == null ? null : switch (digits(raw).replace('ي', 'ی').trim().toUpperCase(Locale.ROOT)) {
            case "1", "NATURAL", "حقیقی", "شخص حقیقی" -> "NATURAL";
            case "2", "LEGAL", "حقوقی", "شخص حقوقی" -> "LEGAL";
            default -> throw invalid("Unresolved taxpayer type in XML.");
        };
        if (natural && legal || type != null && (type.equals("NATURAL") && legal || type.equals("LEGAL") && natural))
            throw invalid("Conflicting taxpayer identity/type requires review.");
        if (type == null) {
            if (natural && legal) throw invalid("Conflicting taxpayer identity/type requires review.");
            if (natural || legal) type = natural ? "NATURAL" : "LEGAL";
            else if (identifier.matches("[0-9]{10}")) type = "NATURAL";
            else if (identifier.matches("[0-9]{11}")) type = "LEGAL";
            else throw invalid("Taxpayer type cannot be resolved; review required.");
            // EF03 identifiers use the contract's fixed national-code/national-ID lengths.
        }
        if (type.equals("NATURAL") && !natural && !identifier.matches("[0-9]{10}"))
            throw invalid("Natural person identity requires review.");
        if (type.equals("LEGAL") && !legal && !identifier.matches("[0-9]{11}"))
            throw invalid("Legal person identity requires review.");
        return type;
    }
}
