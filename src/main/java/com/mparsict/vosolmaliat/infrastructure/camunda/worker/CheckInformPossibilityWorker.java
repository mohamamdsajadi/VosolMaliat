package com.mparsict.vosolmaliat.infrastructure.camunda.worker;

import com.mparsict.vosolmaliat.application.service.InformProcessService;
import com.mparsict.vosolmaliat.infrastructure.camunda.ProcessConstants;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import io.camunda.client.annotation.JobWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CheckInformPossibilityWorker {

    private final InformProcessService informProcessService;

    @JobWorker(type = ProcessConstants.JOB_CHECK_INFORM)
    public void handle(JobClient client, ActivatedJob job) {
        try {
            Long executionPaperId = requiredLong(job, ProcessConstants.VAR_EXECUTION_PAPER_ID);
            client.newCompleteCommand(job.getKey())
                    .variables(informProcessService.checkInformPossibility(executionPaperId))
                    .send()
                    .join();
        } catch (Exception ex) {
            log.error("Error in CheckInformPossibilityWorker", ex);
            fail(client, job, ex);
        }
    }

    private Long requiredLong(ActivatedJob job, String variable) {
        Object value = job.getVariablesAsMap().get(variable);
        if (value == null || "null".equals(String.valueOf(value)) || String.valueOf(value).isBlank()) {
            throw new IllegalStateException("Required process variable is missing: " + variable);
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private void fail(JobClient client, ActivatedJob job, Exception ex) {
        try {
            client.newFailCommand(job.getKey()).retries(job.getRetries() - 1).errorMessage(ex.getMessage()).send().join();
        } catch (Exception failEx) {
            log.warn("Could not send fail command for job key={}: {}", job.getKey(), failEx.getMessage());
        }
    }
}
