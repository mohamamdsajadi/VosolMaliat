package com.mparsict.vosolmaliat.application.service;

import com.mparsict.vosolmaliat.application.dto.request.DebtPaperIssuanceRequest;
import com.mparsict.vosolmaliat.application.dto.request.DebtPaperPushRequest;
import com.mparsict.vosolmaliat.common.exception.DebtPaperPushException;
import com.mparsict.vosolmaliat.domain.model.masterdata.TaxUnit;
import com.mparsict.vosolmaliat.domain.repository.TaxAffairsOfficeRepository;
import com.mparsict.vosolmaliat.domain.repository.TaxUnitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DebtPaperPushMapper {

    private final DebtPaperXmlReader xmlReader;
    private final TaxAffairsOfficeRepository taxAffairsOfficeRepository;
    private final TaxUnitRepository taxUnitRepository;

    public DebtPaperIssuanceRequest map(DebtPaperPushRequest source) {
        Map<String, String> xml = xmlReader.read(source.xml());
        String taxpayerType = taxpayerType(xml);
        Long taxUnitId = requiredLong(xml, "taxpayer.taxunitid", "taxunitid", "kelaseid_tbz");
        TaxUnit taxUnit = taxUnitRepository.findById(taxUnitId)
                .orElseThrow(() -> invalid("Tax unit referenced by Debt Paper XML was not found."));

        if (!taxAffairsOfficeRepository.existsById(source.taxOfficeId())) {
            throw invalid("taxOfficeID does not identify an existing Tax Affairs Office.");
        }
        if (!source.taxOfficeId().equals(taxUnit.getTaxAffairsOfficeId())) {
            throw invalid("Debt Paper XML tax unit does not belong to taxOfficeID.");
        }

        String xmlNationalId = taxpayerIdentifier(xml, taxpayerType);
        if (xmlNationalId != null && !source.nationalID().equals(xmlNationalId)) {
            throw invalid("nationalID does not match the taxpayer identifier in Debt Paper XML.");
        }

        DebtPaperIssuanceRequest.TaxpayerInfo taxpayer = new DebtPaperIssuanceRequest.TaxpayerInfo(
                taxpayerType,
                "NATURAL".equals(taxpayerType) ? source.nationalID() : null,
                first(xml, "taxpayer.fida", "fida"),
                first(xml, "taxpayer.firstname", "firstname", "fname_tbz"),
                first(xml, "taxpayer.lastname", "lastname", "lname_tbz"),
                first(xml, "taxpayer.fathername", "fathername"),
                first(xml, "taxpayer.birthdate", "birthdate", "birthdate_tbz"),
                first(xml, "taxpayer.postalcode", "postalcode", "postalcode_tbz", "postcode_tbz", "zipcode_tbz"),
                first(xml, "taxpayer.address", "address", "address_tbz", "taxpayeraddress_tbz", "tbz_moadiaddress"),
                "LEGAL".equals(taxpayerType) ? source.nationalID() : null,
                first(xml, "taxpayer.registrationnumber", "registrationnumber", "sno_tbz"),
                first(xml, "taxpayer.registrationdate", "registrationdate"),
                first(xml, "taxpayer.companyname", "companyname", "tbz_business_reputation")
        );

        Double taxAmount = parseAmount(source.taxAmount());
        DebtPaperIssuanceRequest.DebtInfo debt = new DebtPaperIssuanceRequest.DebtInfo(
                taxAmount, 0.0, 0.0, 0.0, 0.0, 0.0,
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0
        );

        return new DebtPaperIssuanceRequest(
                source.referenceId(),
                source.produceDate().toString(),
                first(xml, "notificationid", "eblagletterno_tbz"),
                first(xml, "finalizationreason", "elatgatiat_tbz"),
                source.taxSource(),
                source.year().toString(),
                source.period().toString(),
                first(xml, "activitytypecode", "code_activity_tbz"),
                first(xml, "activitytypetitle", "activity_title_tbz", "activitytitle_tbz"),
                taxUnitRepository.findGeneralDirectorateIdByTaxUnitId(taxUnitId).orElse(null),
                source.taxOfficeId(),
                taxUnitId,
                null,
                taxpayer,
                debt,
                null
        );
    }

    private String taxpayerType(Map<String, String> xml) {
        String value = first(xml, "taxpayer.type", "taxpayertype", "bedehitype");
        if (value == null) {
            throw invalid("Taxpayer type is missing from Debt Paper XML.");
        }
        return switch (value.trim().toUpperCase()) {
            case "1", "NATURAL" -> "NATURAL";
            case "2", "LEGAL" -> "LEGAL";
            default -> throw invalid("Taxpayer type in Debt Paper XML is invalid.");
        };
    }

    private String taxpayerIdentifier(Map<String, String> xml, String type) {
        if ("LEGAL".equals(type)) {
            return first(xml, "taxpayer.nationalid", "nationalid", "economiccode_tbz", "nationalid_tbz");
        }
        return first(xml, "taxpayer.nationalcode", "nationalcode", "codemelli_tbz", "nationalid_tbz");
    }

    private Long requiredLong(Map<String, String> xml, String... keys) {
        String value = first(xml, keys);
        if (value == null) {
            throw invalid("Tax unit identifier is missing from Debt Paper XML.");
        }
        try {
            return Long.valueOf(value.replace(",", "").trim());
        } catch (NumberFormatException exception) {
            throw invalid("Tax unit identifier in Debt Paper XML is invalid.");
        }
    }

    private Double parseAmount(String value) {
        if (value == null || value.isBlank()) {
            return 0.0;
        }
        try {
            double amount = new BigDecimal(value.replace(",", "").trim()).doubleValue();
            if (!Double.isFinite(amount)) {
                throw new NumberFormatException("Amount is outside the supported range");
            }
            return amount;
        } catch (NumberFormatException exception) {
            throw invalid("taxAmount is invalid.");
        }
    }

    private String first(Map<String, String> values, String... keys) {
        for (String key : keys) {
            String normalizedKey = key.toLowerCase();
            String value = values.get(normalizedKey);
            if (value == null && normalizedKey.contains(".")) {
                value = values.entrySet().stream()
                        .filter(entry -> entry.getKey().endsWith("." + normalizedKey))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElse(null);
            }
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private DebtPaperPushException invalid(String message) {
        return new DebtPaperPushException(HttpStatus.BAD_REQUEST, -7, message);
    }
}
