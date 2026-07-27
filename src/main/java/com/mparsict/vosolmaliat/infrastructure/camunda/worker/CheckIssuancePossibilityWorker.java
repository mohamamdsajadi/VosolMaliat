package com.mparsict.vosolmaliat.infrastructure.camunda.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.request.DebtPaperIssuanceRequest;
import com.mparsict.vosolmaliat.application.service.RuleEngineService;
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
public class CheckIssuancePossibilityWorker {

    private final RuleEngineService ruleEngineService;
    private final ObjectMapper objectMapper;

    @JobWorker(type = ProcessConstants.JOB_CHECK_ISSUANCE, autoComplete = false)
    public void handle(final JobClient client, final ActivatedJob job) {
        log.debug("Processing job: check-issuance-possibility, key={}", job.getKey());
        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            Object payload = variables.get(ProcessConstants.VAR_DEBT_PAPER_PAYLOAD);
            DebtPaperIssuanceRequest request = toRequest(payload);
            RuleEngineService.RuleEvaluationResult result = ruleEngineService.evaluate(request);

            Map<String, Object> outputVars = new HashMap<>();
            outputVars.put(ProcessConstants.VAR_CAN_ISSUE, result.canIssue());

            if (!result.canIssue()) {
                outputVars.put(ProcessConstants.VAR_REJECTION_CODE, result.rejectionReasonCode());
                outputVars.put(ProcessConstants.VAR_REJECTION_DESC, result.rejectionReasonDescription());
                outputVars.put(ProcessConstants.VAR_RESTRICTION_REMOVAL_DATE, result.restrictionRemovalDate());
                log.info("Issuance blocked: code={}", result.rejectionReasonCode());
            } else {
                log.info("Issuance approved for job key={}", job.getKey());
            }

            client.newCompleteCommand(job.getKey()).variables(outputVars).send().join();
        } catch (Exception e) {
            log.error("Error in CheckIssuancePossibilityWorker", e);
            WorkerFailureHandler.failSafely(client, job, e, log);
        }
    }

    private DebtPaperIssuanceRequest toRequest(Object payload) throws Exception {
        if (payload instanceof String json) {
            return objectMapper.readValue(json, DebtPaperIssuanceRequest.class);
        }
        return objectMapper.convertValue(payload, DebtPaperIssuanceRequest.class);
    }
}
