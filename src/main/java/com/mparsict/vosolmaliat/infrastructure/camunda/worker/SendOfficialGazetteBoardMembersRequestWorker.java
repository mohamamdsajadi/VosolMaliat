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
            Long executionPaperId = resolveExecutionPaperId(variables);
            fakeOfficialGazetteService.send(
                    requestId,
                    executionPaperId,
                    Tabsare3186WorkerVariables.requiredStringOrNested(
                            variables,
                            ProcessConstants.VAR_LEGAL_PERSON_NATIONAL_ID,
                            ProcessConstants.VAR_DEBT_PAPER_PAYLOAD,
                            "taxpayer",
                            "nationalId"
                    ),
                    Tabsare3186WorkerVariables.normalizeJalaliDate(
                            Tabsare3186WorkerVariables.requiredStringOrNested(
                                    variables,
                                    ProcessConstants.VAR_DEBT_PAPER_DATE,
                                    ProcessConstants.VAR_DEBT_PAPER_PAYLOAD,
                                    "debtPaperIssueDate"
                            )
                    )
            );
            client.newCompleteCommand(job.getKey())
                    .variables(Map.of(
                            ProcessConstants.VAR_OFFICIAL_GAZETTE_REQUEST_ID, requestId,
                            ProcessConstants.VAR_EXECUTION_PAPER_ID, executionPaperId
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

    private Long resolveExecutionPaperId(Map<String, Object> variables) {
        Object value = variables.get(ProcessConstants.VAR_EXECUTION_PAPER_ID);
        if (value != null) {
            return value instanceof Number number
                    ? number.longValue()
                    : Long.valueOf(String.valueOf(value));
        }
        String debtPaperUniqueCode = Tabsare3186WorkerVariables.requiredStringOrNested(
                variables,
                ProcessConstants.VAR_EXECUTION_PAPER_ID,
                ProcessConstants.VAR_DEBT_PAPER_PAYLOAD,
                "debtPaperUniqueCode"
        );
        return executionPaperRepository
                .findByFinalizedDebtPaperDebtPaperUniqueCode(debtPaperUniqueCode)
                .map(executionPaper -> executionPaper.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Could not resolve executionPaperId for debtPaperUniqueCode: "
                                + debtPaperUniqueCode
                ));
    }
}
