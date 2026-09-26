package com.mparsict.vosolmaliat.infrastructure.camunda.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.service.RelatedPersonService;
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
public class StoreRelatedPersonsWorker {

    private final RelatedPersonService relatedPersonService;
    private final ExecutionPaperRepository executionPaperRepository;
    private final ObjectMapper objectMapper;

    @JobWorker(type = ProcessConstants.JOB_STORE_RELATED_PERSONS, autoComplete = false)
    public void handle(JobClient client, ActivatedJob job) {
        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            relatedPersonService.storeOfficialGazetteRelatedPersons(
                    ExecutionPaperWorkerVariables.resolveExecutionPaperId(
                            variables,
                            executionPaperRepository
                    ),
                    Tabsare3186WorkerVariables.requiredRelatedPersons(
                            objectMapper,
                            variables,
                            ProcessConstants.VAR_FILTERED_RELATED_PERSONS
                    )
            );
            client.newCompleteCommand(job.getKey()).send().join();
        } catch (Exception ex) {
            log.error("Error storing Official Gazette related persons", ex);
            WorkerFailureHandler.failSafely(client, job, ex, log);
        }
    }
}
