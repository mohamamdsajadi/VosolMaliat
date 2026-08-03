package com.mparsict.vosolmaliat.infrastructure.camunda.worker;

import com.mparsict.vosolmaliat.application.service.FakeSanimDebtInquiryService;
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
public class GetDebtFromSanimWorker {

    private final FakeSanimDebtInquiryService fakeSanimDebtInquiryService;
    private final ExecutionPaperRepository executionPaperRepository;

    @JobWorker(type = ProcessConstants.JOB_GET_DEBT_FROM_SANIM, autoComplete = false)
    public void handle(JobClient client, ActivatedJob job) {
        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            Long executionPaperId = ExecutionPaperWorkerVariables.resolveExecutionPaperId(
                    variables,
                    executionPaperRepository
            );
            String requestId = resolveRequestId(variables, job.getProcessInstanceKey());
            fakeSanimDebtInquiryService.send(requestId, executionPaperId);
            client.newCompleteCommand(job.getKey())
                    .variables(Map.of(
                            ProcessConstants.VAR_EXECUTION_PAPER_ID, executionPaperId,
                            ProcessConstants.VAR_DEBT_INQUIRY_REQUEST_ID, requestId
                    ))
                    .send()
                    .join();
        } catch (Exception ex) {
            log.error("Error sending debt inquiry to fake SANIM", ex);
            WorkerFailureHandler.failSafely(client, job, ex, log);
        }
    }

    private String resolveRequestId(Map<String, Object> variables, long processInstanceKey) {
        Object value = variables.get(ProcessConstants.VAR_DEBT_INQUIRY_REQUEST_ID);
        if (value != null && !String.valueOf(value).isBlank()) {
            return String.valueOf(value).trim();
        }
        return "debt-inquiry-" + processInstanceKey;
    }
}
