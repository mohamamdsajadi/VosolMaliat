package com.mparsict.vosolmaliat.presentation.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.request.DebtPaperIssuanceRequest;
import com.mparsict.vosolmaliat.application.dto.response.IssuanceResponse;
import com.mparsict.vosolmaliat.application.service.IssuanceRequestService;
import com.mparsict.vosolmaliat.common.util.HashUtil;
import com.mparsict.vosolmaliat.domain.model.rule.ExecutionPaperIssuanceRequest;
import com.mparsict.vosolmaliat.infrastructure.camunda.service.CamundaProcessService;
import jakarta.validation.Valid;
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
    public ResponseEntity<?> issue(@RequestBody @Valid DebtPaperIssuanceRequest request) {
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
}
