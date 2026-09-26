package com.mparsict.vosolmaliat.infrastructure.camunda.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.integration.CompanyRegistryRelatedPersonData;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperRepository;
import com.mparsict.vosolmaliat.infrastructure.camunda.ProcessConstants;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FilterRelatedPersonsByDebtPaperDateWorker {

    private final ObjectMapper objectMapper;
    private final ExecutionPaperRepository executionPaperRepository;

    @JobWorker(
            type = ProcessConstants.JOB_FILTER_RELATED_PERSONS_BY_DEBT_PAPER_DATE,
            autoComplete = false
    )
    public void handle(JobClient client, ActivatedJob job) {
        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            String debtPaperDate = Tabsare3186WorkerVariables.normalizeJalaliDate(
                    ExecutionPaperWorkerVariables.resolveDebtPaperDate(
                            variables,
                            executionPaperRepository
                    )
            );
            List<CompanyRegistryRelatedPersonData> filtered =
                    Tabsare3186WorkerVariables.requiredRelatedPersons(
                                    objectMapper,
                                    variables,
                                    ProcessConstants.VAR_OFFICIAL_GAZETTE_RELATED_PERSONS
                            ).stream()
                            .filter(person -> Tabsare3186WorkerVariables.activeAt(person, debtPaperDate))
                            .toList();

            client.newCompleteCommand(job.getKey())
                    .variables(Map.of(ProcessConstants.VAR_FILTERED_RELATED_PERSONS, filtered))
                    .send()
                    .join();
        } catch (Exception ex) {
            log.error("Error filtering related persons by debt-paper date", ex);
            WorkerFailureHandler.failSafely(client, job, ex, log);
        }
    }
}
