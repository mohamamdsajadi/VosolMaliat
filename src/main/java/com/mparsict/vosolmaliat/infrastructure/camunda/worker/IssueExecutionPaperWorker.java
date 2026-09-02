package com.mparsict.vosolmaliat.infrastructure.camunda.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.request.DebtPaperIssuanceRequest;
import com.mparsict.vosolmaliat.application.service.ExecutionPaperService;
import com.mparsict.vosolmaliat.application.service.IssuanceRequestService;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaper;
import com.mparsict.vosolmaliat.infrastructure.camunda.ProcessConstants;
import com.mparsict.vosolmaliat.infrastructure.camunda.service.ExecutionPaperWorkflowVariablesFactory;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import io.camunda.client.annotation.JobWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * برگ اجرا را ایجاد و متغیرهای مشترک فرایندهای ابلاغ و تبصره ۳ ماده ۱۸۶ را منتشر می‌کند.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IssueExecutionPaperWorker {

    private final ExecutionPaperService executionPaperService;
    private final IssuanceRequestService issuanceRequestService;
    private final ExecutionPaperWorkflowVariablesFactory workflowVariablesFactory;
    private final ObjectMapper objectMapper;

    @JobWorker(type = ProcessConstants.JOB_ISSUE_PAPER, autoComplete = false)
    public void handle(final JobClient client, final ActivatedJob job) {
        log.debug("Processing job: {}, key={}", ProcessConstants.JOB_ISSUE_PAPER, job.getKey());
        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            Object payload = variables.get(ProcessConstants.VAR_DEBT_PAPER_PAYLOAD);
            Long requestId = ((Number) variables.get(ProcessConstants.VAR_ISSUANCE_REQUEST_ID)).longValue();

            DebtPaperIssuanceRequest request = toRequest(payload);
            ExecutionPaper ep = executionPaperService.createFromRequest(request);
            Map<String, Object> outputVars = workflowVariablesFactory.create(ep, request);

            log.info("ExecutionPaper issued: code={}, requestId={}", ep.getExecutionPaperUniqueCode(), requestId);
            client.newCompleteCommand(job.getKey()).variables(outputVars).send().join();
        } catch (Exception e) {
            log.error("Error in IssueExecutionPaperWorker", e);
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
