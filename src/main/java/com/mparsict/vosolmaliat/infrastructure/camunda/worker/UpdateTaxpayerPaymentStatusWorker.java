package com.mparsict.vosolmaliat.infrastructure.camunda.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.response.SanimDebtStatusResponse;
import com.mparsict.vosolmaliat.application.service.DebtStatusUpdateService;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperRepository;
import com.mparsict.vosolmaliat.infrastructure.camunda.ProcessConstants;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateTaxpayerPaymentStatusWorker {

    private final DebtStatusUpdateService debtStatusUpdateService;
    private final ExecutionPaperRepository executionPaperRepository;
    private final ObjectMapper objectMapper;

    @JobWorker(type = ProcessConstants.JOB_UPDATE_TAXPAYER_PAYMENT_STATUS, autoComplete = false)
    public void handle(JobClient client, ActivatedJob job) {
        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            Long executionPaperId = ExecutionPaperWorkerVariables.resolveExecutionPaperId(
                    variables,
                    executionPaperRepository
            );
            String requestId = Tabsare3186WorkerVariables.requiredString(
                    variables,
                    ProcessConstants.VAR_DEBT_INQUIRY_REQUEST_ID
            );
            Object rawStatus = variables.get(ProcessConstants.VAR_SANIM_DEBT_STATUS);
            if (rawStatus == null) {
                throw new IllegalArgumentException(
                        "Missing required process variable: " + ProcessConstants.VAR_SANIM_DEBT_STATUS
                );
            }
            SanimDebtStatusResponse status = objectMapper.convertValue(rawStatus, SanimDebtStatusResponse.class);
            debtStatusUpdateService.update(executionPaperId, requestId, status);
            client.newCompleteCommand(job.getKey())
                    .variables(Map.of(ProcessConstants.VAR_DEBT_STATUS_UPDATED, true))
                    .send()
                    .join();
        } catch (Exception ex) {
            log.error("Error updating taxpayer payment status", ex);
            WorkerFailureHandler.failSafely(client, job, ex, log);
        }
    }
}
