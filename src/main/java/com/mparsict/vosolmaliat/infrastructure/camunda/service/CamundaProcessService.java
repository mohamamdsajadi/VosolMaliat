package com.mparsict.vosolmaliat.infrastructure.camunda.service;

import com.mparsict.vosolmaliat.application.dto.response.IssuanceResponse;
import com.mparsict.vosolmaliat.common.exception.BusinessException;
import com.mparsict.vosolmaliat.infrastructure.camunda.ProcessConstants;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceResult;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CamundaProcessService {

    private static final Logger LOG = LoggerFactory.getLogger(CamundaProcessService.class);

    private final CamundaClient camundaClient;

    public IssuanceResponse startExecutionPaperIssuanceProcess(
            Long issuanceRequestId,
            String debtPaperPayload) {
        try {
            Map<String, Object> processVariables = new HashMap<>();
            processVariables.put(ProcessConstants.VAR_ISSUANCE_REQUEST_ID, issuanceRequestId);
            processVariables.put(ProcessConstants.VAR_DEBT_PAPER_PAYLOAD, debtPaperPayload);

            ProcessInstanceResult result = camundaClient
                    .newCreateInstanceCommand()
                    .bpmnProcessId(ProcessConstants.PROCESS_ID)
                    .latestVersion()
                    .variables(processVariables)
                    .withResult()
                    .requestTimeout(Duration.ofSeconds(30))
                    .send()
                    .join();

            Map<String, Object> resultVars = result.getVariablesAsMap();
            boolean success = Boolean.TRUE.equals(resultVars.get(ProcessConstants.VAR_SUCCESS));

            if (success) {
                return new IssuanceResponse(
                        true,
                        (String) resultVars.get(ProcessConstants.VAR_EXECUTION_PAPER_CODE),
                        null,
                        null,
                        null,
                        "برگ اجرا با موفقیت صادر شد"
                );
            } else {
                return new IssuanceResponse(
                        false,
                        null,
                        (String) resultVars.get(ProcessConstants.VAR_REJECTION_CODE),
                        (String) resultVars.get(ProcessConstants.VAR_REJECTION_DESC),
                        (String) resultVars.get(ProcessConstants.VAR_RESTRICTION_REMOVAL_DATE),
                        "درخواست به دلیل محدودیت‌های تعریف شده رد شد"
                );
            }
        } catch (Exception e) {
            LOG.error("Error starting execution paper issuance process", e);
            throw new BusinessException(
                    "PROCESS_START_FAILED",
                    "خطا در راه‌اندازی فرایند صدور برگ اجرا: " + e.getMessage()
            );
        }
    }
}
