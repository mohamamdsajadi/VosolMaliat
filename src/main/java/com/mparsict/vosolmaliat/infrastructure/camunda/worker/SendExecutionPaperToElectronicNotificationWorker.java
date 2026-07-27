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

@Slf4j
@Component
@RequiredArgsConstructor
public class SendExecutionPaperToElectronicNotificationWorker {

    private final InformProcessService informProcessService;
    private final ExecutionPaperRepository executionPaperRepository;

    @JobWorker(type = ProcessConstants.JOB_SEND_TO_ELECTRONIC_NOTIFICATION, autoComplete = false)
    public void handle(JobClient client, ActivatedJob job) {
        try {
            Long executionPaperId = ExecutionPaperWorkerVariables.resolveExecutionPaperId(
                    job.getVariablesAsMap(),
                    executionPaperRepository
            );
            informProcessService.markSentToElectronicNotification(executionPaperId);
            client.newCompleteCommand(job.getKey()).send().join();
        } catch (Exception ex) {
            log.error("Error in SendExecutionPaperToElectronicNotificationWorker", ex);
            WorkerFailureHandler.failSafely(client, job, ex, log);
        }
    }

}
