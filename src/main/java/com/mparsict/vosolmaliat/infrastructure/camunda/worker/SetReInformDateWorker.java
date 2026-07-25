package com.mparsict.vosolmaliat.infrastructure.camunda.worker;

import com.mparsict.vosolmaliat.application.service.InformProcessService;
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
public class SetReInformDateWorker {

    private final InformProcessService informProcessService;

    @JobWorker(type = ProcessConstants.JOB_SET_RE_INFORM_DATE, autoComplete = false)
    public void handle(JobClient client, ActivatedJob job) {
        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            Long executionPaperId = requiredLong(variables, ProcessConstants.VAR_EXECUTION_PAPER_ID);
            String reInformDate = informProcessService.setReInformDate(
                    executionPaperId,
                    (String) variables.get(ProcessConstants.VAR_RESTRICTION_REMOVAL_DATE)
            );
            client.newCompleteCommand(job.getKey())
                    .variables(Map.of(ProcessConstants.VAR_RE_INFORM_DATE, reInformDate))
                    .send()
                    .join();
        } catch (Exception ex) {
            log.error("Error in SetReInformDateWorker", ex);
            WorkerFailureHandler.failSafely(client, job, ex, log);
        }
    }

    private Long requiredLong(Map<String, Object> variables, String variable) {
        Object value = variables.get(variable);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }
}
