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

/**
 * برگ اجرا را ایجاد و متغیرهای مشترک فرایندهای ابلاغ و تبصره ۳ ماده ۱۸۶ را منتشر می‌کند.
 */
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
        log.debug("Processing job: {}, key={}", ProcessConstants.JOB_ISSUE_PAPER, job.getKey());
        try {
            Map<String, Object> variables = job.getVariablesAsMap();
            Object payload = variables.get(ProcessConstants.VAR_DEBT_PAPER_PAYLOAD);
            Long requestId = ((Number) variables.get(ProcessConstants.VAR_ISSUANCE_REQUEST_ID)).longValue();

            DebtPaperIssuanceRequest request = toRequest(payload);
            Map<String, Object> outputVars = new HashMap<>();
            addOrganizationVariables(outputVars, request.taxUnitId());
            addTabsare3186Variables(outputVars, request);

            ExecutionPaper ep = executionPaperService.createFromRequest(request);
            outputVars.put(ProcessConstants.VAR_EXECUTION_PAPER_ID, ep.getId());
            outputVars.put(ProcessConstants.VAR_EXECUTION_PAPER_CODE, ep.getExecutionPaperUniqueCode());
            outputVars.put(ProcessConstants.VAR_TAX_UNIT_ID, ep.getTaxUnitId());
            outputVars.put(ProcessConstants.VAR_DEBT_PAPER_PAYLOAD, request);
            outputVars.put(ProcessConstants.VAR_SUCCESS, true);

            log.info("ExecutionPaper issued: code={}, requestId={}", ep.getExecutionPaperUniqueCode(), requestId);
            client.newCompleteCommand(job.getKey()).variables(outputVars).send().join();
        } catch (Exception e) {
            log.error("Error in IssueExecutionPaperWorker", e);
            WorkerFailureHandler.failSafely(client, job, e, log);
        }
    }

    private DebtPaperIssuanceRequest toRequest(Object payload) throws Exception {
        if (payload instanceof String json) {
            return objectMapper.readValue(json, DebtPaperIssuanceRequest.class);
        }
        return objectMapper.convertValue(payload, DebtPaperIssuanceRequest.class);
    }

    /**
     * برای مودی حقوقی، شناسه ملی و تاریخ برگ بدهی را به فرایند تبصره می‌دهد.
     */
    private void addTabsare3186Variables(
            Map<String, Object> outputVars,
            DebtPaperIssuanceRequest request) {
        if (!"LEGAL".equalsIgnoreCase(request.taxpayer().type().trim())) {
            return;
        }
        String nationalId = request.taxpayer().nationalId();
        if (nationalId == null || nationalId.isBlank()) {
            throw new BusinessException(
                    "LEGAL_PERSON_NATIONAL_ID_REQUIRED",
                    "شناسه ملی مودی حقوقی برای فرایند تبصره ۳ ماده ۱۸۶ الزامی است"
            );
        }
        String debtPaperDate = request.debtPaperIssueDate();
        if (debtPaperDate == null || debtPaperDate.isBlank()) {
            throw new BusinessException(
                    "DEBT_PAPER_DATE_REQUIRED",
                    "تاریخ برگ بدهی برای فرایند تبصره ۳ ماده ۱۸۶ الزامی است"
            );
        }
        outputVars.put(ProcessConstants.VAR_LEGAL_PERSON_NATIONAL_ID, nationalId.trim());
        outputVars.put(ProcessConstants.VAR_DEBT_PAPER_DATE, debtPaperDate.trim());
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
