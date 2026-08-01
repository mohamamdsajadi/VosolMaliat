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

import java.util.HashMap;
import java.util.Map;

/**
 * امکان ابلاغ را بررسی و متغیر Boolean به نام canInform را برای Gateway تولید می‌کند.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CheckInformPossibilityWorker {

    private final InformProcessService informProcessService;
    private final ExecutionPaperRepository executionPaperRepository;

    @JobWorker(type = ProcessConstants.JOB_CHECK_INFORM, autoComplete = false)
    public void handle(JobClient client, ActivatedJob job) {
        try {
            Long executionPaperId = ExecutionPaperWorkerVariables.resolveExecutionPaperId(
                    job.getVariablesAsMap(),
                    executionPaperRepository
            );
            Map<String, Object> outputVariables = new HashMap<>(
                    informProcessService.checkInformPossibility(executionPaperId)
            );
            Object canInform = outputVariables.get(ProcessConstants.VAR_CAN_INFORM);
            if (!(canInform instanceof Boolean)) {
                throw new IllegalArgumentException(
                        "Inform possibility result must contain BOOLEAN process variable: "
                                + ProcessConstants.VAR_CAN_INFORM
                );
            }
            outputVariables.put(ProcessConstants.VAR_EXECUTION_PAPER_ID, executionPaperId);
            client.newCompleteCommand(job.getKey())
                    .variables(outputVariables)
                    .send()
                    .join();
        } catch (Exception ex) {
            log.error("Error in CheckInformPossibilityWorker", ex);
            WorkerFailureHandler.failSafely(client, job, ex, log);
        }
    }

}
