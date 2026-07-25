package com.mparsict.vosolmaliat.infrastructure.camunda.worker;

import com.mparsict.vosolmaliat.application.service.FakeSanimInformService;
import com.mparsict.vosolmaliat.infrastructure.camunda.ProcessConstants;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import io.camunda.client.annotation.JobWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SendExecutionPaperIdToSanimWorker {

    private final FakeSanimInformService fakeSanimInformService;

    @JobWorker(type = ProcessConstants.JOB_SEND_EXECUTION_PAPER_ID_TO_SANIM, autoComplete = false)
    public void handle(JobClient client, ActivatedJob job) {
        try {
            Long executionPaperId = requiredLong(
                    job.getVariablesAsMap(),
                    ProcessConstants.VAR_EXECUTION_PAPER_ID
            );
            fakeSanimInformService.sendExecutionPaperId(executionPaperId);

            log.info("Execution paper id sent to fake SANIM: executionPaperId={}", executionPaperId);
            client.newCompleteCommand(job.getKey()).send().join();
        } catch (Exception ex) {
            log.error("Error in SendExecutionPaperIdToSanimWorker", ex);
            WorkerFailureHandler.failSafely(client, job, ex, log);
        }
    }

    private Long requiredLong(Map<String, Object> variables, String variable) {
        Object value = variables.get(variable);
        if (value == null) {
            throw new IllegalArgumentException("Missing required process variable: " + variable);
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }
}
