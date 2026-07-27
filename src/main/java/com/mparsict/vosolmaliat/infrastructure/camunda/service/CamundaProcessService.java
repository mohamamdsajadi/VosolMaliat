package com.mparsict.vosolmaliat.infrastructure.camunda.service;

import com.mparsict.vosolmaliat.application.dto.request.DebtPaperIssuanceRequest;
import com.mparsict.vosolmaliat.application.dto.response.IssuanceResponse;
import com.mparsict.vosolmaliat.application.dto.request.Tabsare3186TestStartRequest;
import com.mparsict.vosolmaliat.application.dto.response.Tabsare3186TestStartResponse;
import com.mparsict.vosolmaliat.common.exception.BusinessException;
import com.mparsict.vosolmaliat.infrastructure.camunda.ProcessConstants;
import io.camunda.client.CamundaClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CamundaProcessService {

    private static final Logger LOG = LoggerFactory.getLogger(CamundaProcessService.class);

    private final CamundaClient camundaClient;

    public IssuanceResponse startExecutionPaperIssuanceProcess(
            Long issuanceRequestId,
            DebtPaperIssuanceRequest debtPaperPayload) {
        try {
            Map<String, Object> processVariables = new HashMap<>();
            processVariables.put(ProcessConstants.VAR_ISSUANCE_REQUEST_ID, issuanceRequestId);
            processVariables.put(ProcessConstants.VAR_DEBT_PAPER_PAYLOAD, debtPaperPayload);

            camundaClient
                    .newCreateInstanceCommand()
                    .bpmnProcessId(ProcessConstants.PROCESS_ID)
                    .latestVersion()
                    .variables(processVariables)
                    .send()
                    .join();

            return new IssuanceResponse(
                    true,
                    null,
                    null,
                    null,
                    null,
                    "درخواست دریافت شد و فرایند صدور برگ اجرا آغاز شد"
            );
        } catch (Exception e) {
            LOG.error("Error starting execution paper issuance process", e);
            throw new BusinessException(
                    "PROCESS_START_FAILED",
                    "خطا در راه‌اندازی فرایند صدور برگ اجرا: " + e.getMessage()
            );
        }
    }

    public Tabsare3186TestStartResponse startTabsare3186TestProcess(Tabsare3186TestStartRequest request) {
        String officialGazetteRequestId = UUID.randomUUID().toString();
        Map<String, Object> variables = new HashMap<>();
        variables.put(ProcessConstants.VAR_EXECUTION_PAPER_ID, request.executionPaperId());
        variables.put(ProcessConstants.VAR_LEGAL_PERSON_NATIONAL_ID, request.legalPersonNationalId().trim());
        variables.put(ProcessConstants.VAR_DEBT_PAPER_DATE, request.debtPaperDate().trim());
        variables.put(ProcessConstants.VAR_OFFICIAL_GAZETTE_REQUEST_ID, officialGazetteRequestId);

        try {
            var processInstance = camundaClient
                    .newCreateInstanceCommand()
                    .bpmnProcessId(ProcessConstants.TABSARE_3_186_PROCESS_ID)
                    .latestVersion()
                    .variables(variables)
                    .send()
                    .join();

            return new Tabsare3186TestStartResponse(
                    processInstance.getProcessInstanceKey(),
                    officialGazetteRequestId,
                    "فرایند آزمایشی تبصره ۳ ماده ۱۸۶ آغاز شد"
            );
        } catch (Exception ex) {
            LOG.error("Error starting Tabsare3-186 test process", ex);
            throw new BusinessException(
                    "TABSARE_3_186_PROCESS_START_FAILED",
                    "خطا در راه‌اندازی فرایند آزمایشی تبصره ۳ ماده ۱۸۶: " + ex.getMessage(),
                    ex
            );
        }
    }
}
