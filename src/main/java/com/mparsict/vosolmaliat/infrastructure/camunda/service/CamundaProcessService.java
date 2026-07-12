package com.mparsict.vosolmaliat.infrastructure.camunda.service;

import com.mparsict.vosolmaliat.application.dto.response.IssuanceResponse;
import com.mparsict.vosolmaliat.common.exception.BusinessException;
import com.mparsict.vosolmaliat.infrastructure.camunda.ProcessConstants;
import io.camunda.client.CamundaClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
}
