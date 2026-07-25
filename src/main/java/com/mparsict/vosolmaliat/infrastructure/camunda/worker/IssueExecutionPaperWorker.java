package com.mparsict.vosolmaliat.infrastructure.camunda.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.request.DebtPaperIssuanceRequest;
import com.mparsict.vosolmaliat.application.service.ExecutionPaperService;
import com.mparsict.vosolmaliat.application.service.IssuanceRequestService;
import com.mparsict.vosolmaliat.application.service.cartable.OrganizationCandidateGroup;
import com.mparsict.vosolmaliat.common.exception.BusinessException;
import com.mparsict.vosolmaliat.domain.enums.BusinessRole;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaper;
import com.mparsict.vosolmaliat.domain.repository.TaxUnitRepository;
import com.mparsict.vosolmaliat.infrastructure.camunda.ProcessConstants;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import io.camunda.client.annotation.JobWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class IssueExecutionPaperWorker {

    private final ExecutionPaperService executionPaperService;
    private final IssuanceRequestService issuanceRequestService;
    private final TaxUnitRepository taxUnitRepository;
    private final ObjectMapper objectMapper;

    @JobWorker(type = ProcessConstants.JOB_ISSUE_PAPER, autoComplete = false)
    public void handle(final JobClient client, final ActivatedJob job) {
        log.debug("Processing job: issue-execution-paper, key={}", job.getKey());
        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            String payload = (String) variables.get(ProcessConstants.VAR_DEBT_PAPER_PAYLOAD);
            Long requestId = ((Number) variables.get(ProcessConstants.VAR_ISSUANCE_REQUEST_ID)).longValue();

            DebtPaperIssuanceRequest request = objectMapper.readValue(payload, DebtPaperIssuanceRequest.class);
            Map<String, Object> outputVars = new HashMap<>();
            addOrganizationVariables(outputVars, request.taxUnitId());

            ExecutionPaper ep = executionPaperService.createFromRequest(request);
            outputVars.put(ProcessConstants.VAR_EXECUTION_PAPER_ID, ep.getId());
            outputVars.put(ProcessConstants.VAR_EXECUTION_PAPER_CODE, ep.getExecutionPaperUniqueCode());
            outputVars.put(ProcessConstants.VAR_TAX_UNIT_ID, ep.getTaxUnitId());
            outputVars.put(ProcessConstants.VAR_SUCCESS, true);

            log.info("ExecutionPaper issued: code={}, requestId={}", ep.getExecutionPaperUniqueCode(), requestId);
            client.newCompleteCommand(job.getKey()).variables(outputVars).send().join();
        } catch (Exception e) {
            log.error("Error in IssueExecutionPaperWorker", e);
            WorkerFailureHandler.failSafely(client, job, e, log);
        }
    }

    private void addOrganizationVariables(Map<String, Object> outputVars, Long taxUnitId) {
        if (taxUnitId == null) {
            throw new BusinessException("EXECUTION_PAPER_ORGANIZATION_REQUIRED", "واحد مالیاتی برگ اجرا مشخص نشده است");
        }
        Long taxAffairsOfficeId = taxUnitRepository.findTaxAffairsOfficeIdByTaxUnitId(taxUnitId)
                .orElseThrow(() -> new BusinessException("TAX_OFFICE_NOT_FOUND_FOR_UNIT", "اداره مالیاتی واحد برگ اجرا یافت نشد"));
        Long generalDirectorateId = taxUnitRepository.findGeneralDirectorateIdByTaxUnitId(taxUnitId)
                .orElseThrow(() -> new BusinessException("GENERAL_OFFICE_NOT_FOUND_FOR_UNIT", "اداره کل مالیاتی واحد برگ اجرا یافت نشد"));

        outputVars.put(ProcessConstants.VAR_TAX_AFFAIRS_OFFICE_ID, taxAffairsOfficeId);
        outputVars.put(ProcessConstants.VAR_GENERAL_DIRECTORATE_ID, generalDirectorateId);
        outputVars.put(ProcessConstants.VAR_GENERAL_OFFICE_HEAD_CANDIDATE_GROUPS,
                List.of(OrganizationCandidateGroup.forOrganization(BusinessRole.GENERAL_OFFICE_HEAD, generalDirectorateId)));
        outputVars.put(ProcessConstants.VAR_TAX_OFFICE_HEAD_CANDIDATE_GROUPS,
                List.of(OrganizationCandidateGroup.forOrganization(BusinessRole.TAX_OFFICE_HEAD, taxAffairsOfficeId)));
        outputVars.put(ProcessConstants.VAR_TAX_UNIT_HEAD_CANDIDATE_GROUPS,
                List.of(OrganizationCandidateGroup.forOrganization(BusinessRole.TAX_UNIT_HEAD, taxUnitId)));
        outputVars.put(ProcessConstants.VAR_TAX_EXPERT_CANDIDATE_GROUPS,
                List.of(OrganizationCandidateGroup.forOrganization(BusinessRole.TAX_EXPERT, taxUnitId)));
    }
}
