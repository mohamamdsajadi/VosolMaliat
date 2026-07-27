package com.mparsict.vosolmaliat.infrastructure.camunda.worker;

import com.mparsict.vosolmaliat.application.service.InformProcessService;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperRepository;
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
public class UpdateInformStatusWorker {

    private final InformProcessService informProcessService;
    private final ExecutionPaperRepository executionPaperRepository;

    @JobWorker(type = ProcessConstants.JOB_UPDATE_INFORM_STATUS, autoComplete = false)
    public void handle(JobClient client, ActivatedJob job) {
        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            Long executionPaperId = ExecutionPaperWorkerVariables.resolveExecutionPaperId(
                    variables,
                    executionPaperRepository
            );
            Boolean canInform = requiredBoolean(variables, ProcessConstants.VAR_CAN_INFORM);
            String informDate = canInform
                    ? Tabsare3186WorkerVariables.requiredString(
                            variables,
                            ProcessConstants.VAR_INFORM_DATE
                    )
                    : null;
            informProcessService.updateInformStatus(
                    executionPaperId,
                    canInform,
                    informDate,
                    (String) variables.get(ProcessConstants.VAR_REJECTION_CODE),
                    (String) variables.get(ProcessConstants.VAR_REJECTION_DESC)
            );
            client.newCompleteCommand(job.getKey()).send().join();
        } catch (Exception ex) {
            log.error("Error in UpdateInformStatusWorker", ex);
            WorkerFailureHandler.failSafely(client, job, ex, log);
        }
    }

    private Boolean requiredBoolean(Map<String, Object> variables, String variable) {
        Object value = variables.get(variable);
        if (!(value instanceof Boolean decision)) {
            throw new IllegalStateException("Required process variable is missing: " + variable);
        }
        return decision;
    }
}
