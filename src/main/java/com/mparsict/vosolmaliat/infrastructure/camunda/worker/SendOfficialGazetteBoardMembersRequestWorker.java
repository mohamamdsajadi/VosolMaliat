package com.mparsict.vosolmaliat.infrastructure.camunda.worker;

import com.mparsict.vosolmaliat.application.service.FakeOfficialGazetteService;
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
public class SendOfficialGazetteBoardMembersRequestWorker {

    private final FakeOfficialGazetteService fakeOfficialGazetteService;
    private final ExecutionPaperRepository executionPaperRepository;

    @JobWorker(
            type = ProcessConstants.JOB_SEND_OFFICIAL_GAZETTE_BOARD_MEMBERS_REQUEST,
            autoComplete = false
    )
    public void handle(JobClient client, ActivatedJob job) {
        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            String requestId = resolveRequestId(variables, job.getProcessInstanceKey());
            Long executionPaperId = ExecutionPaperWorkerVariables.resolveExecutionPaperId(
                    variables,
                    executionPaperRepository
            );
            String legalPersonNationalId = Tabsare3186WorkerVariables.requiredStringOrNested(
                    variables,
                    ProcessConstants.VAR_LEGAL_PERSON_NATIONAL_ID,
                    ProcessConstants.VAR_DEBT_PAPER_PAYLOAD,
                    "taxpayer",
                    "nationalId"
            );
            String debtPaperDate = Tabsare3186WorkerVariables.normalizeJalaliDate(
                    ExecutionPaperWorkerVariables.resolveDebtPaperDate(
                            variables,
                            executionPaperRepository
                    )
            );
            fakeOfficialGazetteService.send(
                    requestId,
                    executionPaperId,
                    legalPersonNationalId,
                    debtPaperDate
            );
            client.newCompleteCommand(job.getKey())
                    .variables(Map.of(
                            ProcessConstants.VAR_OFFICIAL_GAZETTE_REQUEST_ID, requestId,
                            ProcessConstants.VAR_EXECUTION_PAPER_ID, executionPaperId,
                            ProcessConstants.VAR_LEGAL_PERSON_NATIONAL_ID, legalPersonNationalId,
                            ProcessConstants.VAR_DEBT_PAPER_DATE, debtPaperDate
                    ))
                    .send()
                    .join();
        } catch (Exception ex) {
            log.error("Error sending Official Gazette board-member request", ex);
            WorkerFailureHandler.failSafely(client, job, ex, log);
        }
    }

    private String resolveRequestId(Map<String, Object> variables, long processInstanceKey) {
        Object value = variables.get(ProcessConstants.VAR_OFFICIAL_GAZETTE_REQUEST_ID);
        if (value != null && !String.valueOf(value).isBlank()) {
            return String.valueOf(value).trim();
        }
        return "official-gazette-" + processInstanceKey;
    }

}
