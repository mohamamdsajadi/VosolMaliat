package com.mparsict.vosolmaliat.infrastructure.camunda.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.service.FakeRestrictionService;
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
public class SendRelatedPersonsForRestrictionsWorker {

    private final FakeRestrictionService fakeRestrictionService;
    private final ExecutionPaperRepository executionPaperRepository;
    private final ObjectMapper objectMapper;

    @JobWorker(
            type = ProcessConstants.JOB_SEND_RELATED_PERSONS_FOR_RESTRICTIONS,
            autoComplete = false
    )
    public void handle(JobClient client, ActivatedJob job) {
        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            fakeRestrictionService.send(
                    Tabsare3186WorkerVariables.requiredString(
                            variables,
                            ProcessConstants.VAR_OFFICIAL_GAZETTE_REQUEST_ID
                    ),
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
            log.error("Error sending related persons for restrictions", ex);
            WorkerFailureHandler.failSafely(client, job, ex, log);
        }
    }
}
