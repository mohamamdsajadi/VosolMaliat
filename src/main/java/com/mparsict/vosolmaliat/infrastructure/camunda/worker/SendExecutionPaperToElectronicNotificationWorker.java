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
public class SendExecutionPaperToElectronicNotificationWorker {

    private final InformProcessService informProcessService;

    @JobWorker(type = ProcessConstants.JOB_SEND_TO_ELECTRONIC_NOTIFICATION)
    public void handle(JobClient client, ActivatedJob job) {
        try {
            Long executionPaperId = requiredLong(job, ProcessConstants.VAR_EXECUTION_PAPER_ID);
            informProcessService.markSentToElectronicNotification(executionPaperId);
            client.newCompleteCommand(job.getKey()).send().join();
        } catch (Exception ex) {
            log.error("Error in SendExecutionPaperToElectronicNotificationWorker", ex);
            client.newFailCommand(job.getKey()).retries(job.getRetries() - 1).errorMessage(ex.getMessage()).send().join();
        }
    }

    private Long requiredLong(ActivatedJob job, String variable) {
        Object value = job.getVariablesAsMap().get(variable);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }
}
