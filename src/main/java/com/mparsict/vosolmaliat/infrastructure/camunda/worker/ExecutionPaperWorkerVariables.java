package com.mparsict.vosolmaliat.infrastructure.camunda.worker;

import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperRepository;
import com.mparsict.vosolmaliat.infrastructure.camunda.ProcessConstants;

import java.util.Map;

final class ExecutionPaperWorkerVariables {

    private ExecutionPaperWorkerVariables() {
    }

    static Long resolveExecutionPaperId(
            Map<String, Object> variables,
            ExecutionPaperRepository executionPaperRepository) {
        Object directId = variables.get(ProcessConstants.VAR_EXECUTION_PAPER_ID);
        if (directId != null && !String.valueOf(directId).isBlank()) {
            return directId instanceof Number number
                    ? number.longValue()
                    : Long.valueOf(String.valueOf(directId));
        }

        Object executionPaperCode = variables.get(ProcessConstants.VAR_EXECUTION_PAPER_CODE);
        if (executionPaperCode != null && !String.valueOf(executionPaperCode).isBlank()) {
            String code = String.valueOf(executionPaperCode).trim();
            return executionPaperRepository.findByExecutionPaperUniqueCode(code)
                    .map(executionPaper -> executionPaper.getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Could not resolve executionPaperId for executionPaperCode: " + code
                    ));
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

    static String resolveDebtPaperDate(
            Map<String, Object> variables,
            ExecutionPaperRepository executionPaperRepository) {
        Object directDate = variables.get(ProcessConstants.VAR_DEBT_PAPER_DATE);
        if (directDate != null && !String.valueOf(directDate).isBlank()) {
            return String.valueOf(directDate).trim();
        }

        Object payload = variables.get(ProcessConstants.VAR_DEBT_PAPER_PAYLOAD);
        if (payload instanceof Map<?, ?> payloadMap) {
            Object nestedDate = payloadMap.get("debtPaperIssueDate");
            if (nestedDate != null && !String.valueOf(nestedDate).isBlank()) {
                return String.valueOf(nestedDate).trim();
            }
        }

        Long executionPaperId = resolveExecutionPaperId(variables, executionPaperRepository);
        return executionPaperRepository
                .findDebtPaperIssueDateByExecutionPaperId(executionPaperId)
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Could not resolve debtPaperDate for executionPaperId: "
                                + executionPaperId
                ));
    }
}
