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
public class SendInformDataToSanimWorker {

    private final InformProcessService informProcessService;
    private final ExecutionPaperRepository executionPaperRepository;

    @JobWorker(type = ProcessConstants.JOB_SEND_INFORM_DATA_TO_SANIM, autoComplete = false)
    public void handle(JobClient client, ActivatedJob job) {
        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            Long executionPaperId = ExecutionPaperWorkerVariables.resolveExecutionPaperId(
                    variables,
                    executionPaperRepository
            );
            informProcessService.sendInformDateToSanim(
                    executionPaperId,
                    Tabsare3186WorkerVariables.requiredString(
                            variables,
                            ProcessConstants.VAR_INFORM_DATE
                    )
            );
            client.newCompleteCommand(job.getKey())
                    .variables(Map.of(ProcessConstants.VAR_EXECUTION_PAPER_ID, executionPaperId))
                    .send()
                    .join();
        } catch (Exception ex) {
            log.error("Error in SendInformDataToSanimWorker", ex);
            WorkerFailureHandler.failSafely(client, job, ex, log);
        }
    }

}
