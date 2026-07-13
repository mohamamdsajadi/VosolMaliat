package com.mparsict.vosolmaliat.infrastructure.camunda.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.request.DebtPaperIssuanceRequest;
import com.mparsict.vosolmaliat.application.service.ExecutionPaperService;
import com.mparsict.vosolmaliat.application.service.IssuanceRequestService;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaper;
import com.mparsict.vosolmaliat.infrastructure.camunda.ProcessConstants;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import io.camunda.client.annotation.JobWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class IssueExecutionPaperWorker {

    private final ExecutionPaperService executionPaperService;
    private final IssuanceRequestService issuanceRequestService;
    private final ObjectMapper objectMapper;

    @JobWorker(type = ProcessConstants.JOB_ISSUE_PAPER)
    public void handle(final JobClient client, final ActivatedJob job) {
        log.debug("Processing job: issue-execution-paper, key={}", job.getKey());
        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            String payload = (String) variables.get(ProcessConstants.VAR_DEBT_PAPER_PAYLOAD);
            Long requestId = ((Number) variables.get(ProcessConstants.VAR_ISSUANCE_REQUEST_ID)).longValue();

            DebtPaperIssuanceRequest request = objectMapper.readValue(payload, DebtPaperIssuanceRequest.class);
            ExecutionPaper ep = executionPaperService.createFromRequest(request);

            Map<String, Object> outputVars = new HashMap<>();
            outputVars.put(ProcessConstants.VAR_EXECUTION_PAPER_ID, ep.getId());
            outputVars.put(ProcessConstants.VAR_EXECUTION_PAPER_CODE, ep.getExecutionPaperUniqueCode());
            outputVars.put(ProcessConstants.VAR_GENERAL_DIRECTORATE_ID, request.mainTaxBureauId());
            outputVars.put(ProcessConstants.VAR_TAX_AFFAIRS_OFFICE_ID, request.taxBureauId());
            outputVars.put(ProcessConstants.VAR_TAX_UNIT_ID, ep.getTaxUnitId());
            outputVars.put(ProcessConstants.VAR_SUCCESS, true);

            log.info("ExecutionPaper issued: code={}, requestId={}", ep.getExecutionPaperUniqueCode(), requestId);
            client.newCompleteCommand(job.getKey()).variables(outputVars).send().join();
        } catch (Exception e) {
            log.error("Error in IssueExecutionPaperWorker", e);
            client.newFailCommand(job.getKey())
                    .retries(job.getRetries() - 1)
                    .errorMessage(e.getMessage())
                    .send()
                    .join();
        }
    }
}
