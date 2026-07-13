package com.mparsict.vosolmaliat.presentation.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.request.DebtPaperIssuanceRequest;
import com.mparsict.vosolmaliat.application.dto.response.IssuanceResponse;
import com.mparsict.vosolmaliat.application.service.IssuanceRequestService;
import com.mparsict.vosolmaliat.common.util.HashUtil;
import com.mparsict.vosolmaliat.domain.model.rule.ExecutionPaperIssuanceRequest;
import com.mparsict.vosolmaliat.infrastructure.camunda.service.CamundaProcessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v1/debt-papers")
@RequiredArgsConstructor
public class IssuanceController {

    private final IssuanceRequestService issuanceRequestService;
    private final CamundaProcessService camundaProcessService;
    private final ObjectMapper objectMapper;

    @PostMapping("/issue")
    public ResponseEntity<?> issue(@RequestBody JsonNode body) {
        DebtPaperIssuanceRequest request = toIssuanceRequest(body);
        String json;
        try {
            json = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize issuance request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "SERIALIZATION_ERROR", "message", e.getMessage()));
        }

        String hash = HashUtil.sha256(json);

        // Duplicate check
        Optional<ExecutionPaperIssuanceRequest> existing = issuanceRequestService.findByHash(hash);
        if (existing.isPresent()) {
            log.warn("Duplicate issuance request rejected, hash={}", hash);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "error", "DUPLICATE_REQUEST",
                            "message", "این درخواست قبلاً ثبت شده است"
                    ));
        }

        // Save the issuance request
        ExecutionPaperIssuanceRequest savedRequest = issuanceRequestService.createAndSave(request);

        // Start Camunda process and wait for result
        IssuanceResponse response = camundaProcessService.startExecutionPaperIssuanceProcess(
                savedRequest.getId(), json);

        if (response.success()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
        }
    }

    private DebtPaperIssuanceRequest toIssuanceRequest(JsonNode body) {
        if (body.hasNonNull("debtPaperUniqueCode")) {
            return objectMapper.convertValue(body, DebtPaperIssuanceRequest.class);
        }

        String taxType = text(body, "bedehitype");
        String nationalCode = firstText(body, "CodeMelli_tbz", "NationalID_tbz");
        String nationalId = firstText(body, "Economiccode_Tbz", "NationalID_tbz");
        String taxpayerType = "2".equals(taxType) ? "LEGAL" : "NATURAL";

        DebtPaperIssuanceRequest.TaxpayerInfo taxpayer = new DebtPaperIssuanceRequest.TaxpayerInfo(
                taxpayerType,
                "LEGAL".equals(taxpayerType) ? null : nationalCode,
                null,
                text(body, "FName_tbz"),
                text(body, "LName_tbz"),
                null,
                text(body, "BirthDate_tbz"),
                firstText(body, "PostalCode_tbz", "PostCode_tbz", "postalCode"),
                firstText(body, "Address_tbz", "address", "TaxpayerAddress_tbz"),
                "LEGAL".equals(taxpayerType) ? nationalId : null,
                text(body, "SNO_tbz"),
                null,
                text(body, "Tbz_Business_reputation")
        );

        DebtPaperIssuanceRequest.DebtInfo debtInfo = new DebtPaperIssuanceRequest.DebtInfo(
                number(body, "tbz_AslMaliat"),
                number(body, "tbz_AslAvarez"),
                number(body, "JarimeSabet_tbz"),
                number(body, "JarimeDirkard_tbz"),
                number(body, "penaltyoff_Tbz"),
                number(body, "penaltycs_Tbz"),
                number(body, "pardakhtoasl_Tbz"),
                number(body, "pardakhtijarime_Tbz"),
                number(body, "Etebar_tbz"),
                number(body, "MandeBedehi_tbz"),
                0.0,
                0.0,
                0.0
        );

        return new DebtPaperIssuanceRequest(
                firstText(body, "ID_REF_PORTAL", "GatiNo_tbz", "EblagLetterNo_tbz"),
                firstText(body, "GatiDate_tbz", "EblagLetterDate_tbz"),
                text(body, "EblagLetterNo_tbz"),
                text(body, "ElatGatiat_tbz"),
                firstText(body, "taxtypecode_Tbz", "tbz_AmalKard"),
                firstText(body, "tbz_Dore", "tbz_AmalKard"),
                text(body, "tbz_Dore"),
                text(body, "CODE_ACTIVITY_tbz"),
                firstText(body, "ACTIVITY_TITLE_tbz", "ActivityTitle_tbz", "activityTypeTitle"),
                longNumber(body, "OmurCode_tbz"),
                longNumber(body, "VahedCode_tbz"),
                longNumber(body, "KelaseID_tbz"),
                longNumber(body, "SenderVahedCode_tbz"),
                taxpayer,
                debtInfo,
                null
        );
    }

    private String firstText(JsonNode body, String... keys) {
        for (String key : keys) {
            String value = text(body, key);
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private String text(JsonNode body, String key) {
        JsonNode value = body.get(key);
        return value == null || value.isNull() ? null : value.asText();
    }

    private Double number(JsonNode body, String key) {
        String value = text(body, key);
        if (value == null || value.isBlank()) return 0.0;
        return Double.valueOf(value.replace(",", ""));
    }

    private Long longNumber(JsonNode body, String key) {
        String value = text(body, key);
        if (value == null || value.isBlank()) return null;
        return Long.valueOf(value.replace(",", ""));
    }
}
