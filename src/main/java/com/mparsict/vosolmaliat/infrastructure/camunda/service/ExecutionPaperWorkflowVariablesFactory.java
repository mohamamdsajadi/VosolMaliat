package com.mparsict.vosolmaliat.infrastructure.camunda.service;

import com.mparsict.vosolmaliat.application.dto.request.DebtPaperIssuanceRequest;
import com.mparsict.vosolmaliat.application.service.cartable.OrganizationCandidateGroup;
import com.mparsict.vosolmaliat.common.exception.BusinessException;
import com.mparsict.vosolmaliat.domain.enums.BusinessRole;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaper;
import com.mparsict.vosolmaliat.domain.repository.TaxUnitRepository;
import com.mparsict.vosolmaliat.infrastructure.camunda.ProcessConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ExecutionPaperWorkflowVariablesFactory {

    private final TaxUnitRepository taxUnitRepository;

    public Map<String, Object> create(ExecutionPaper paper, DebtPaperIssuanceRequest request) {
        Map<String, Object> variables = new HashMap<>();
        addOrganizationVariables(variables, paper.getTaxUnitId());
        addLegalTaxpayerVariables(variables, request);
        variables.put(ProcessConstants.VAR_EXECUTION_PAPER_ID, paper.getId());
        variables.put(ProcessConstants.VAR_EXECUTION_PAPER_CODE, paper.getExecutionPaperUniqueCode());
        variables.put(ProcessConstants.VAR_TAX_UNIT_ID, paper.getTaxUnitId());
        variables.put(ProcessConstants.VAR_DEBT_PAPER_PAYLOAD, request);
        variables.put(ProcessConstants.VAR_SUCCESS, true);
        return variables;
    }

    private void addLegalTaxpayerVariables(Map<String, Object> variables, DebtPaperIssuanceRequest request) {
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
        variables.put(ProcessConstants.VAR_LEGAL_PERSON_NATIONAL_ID, nationalId.trim());
        variables.put(ProcessConstants.VAR_DEBT_PAPER_DATE, debtPaperDate.trim());
    }

    private void addOrganizationVariables(Map<String, Object> variables, Long taxUnitId) {
        if (taxUnitId == null) {
            throw new BusinessException("EXECUTION_PAPER_ORGANIZATION_REQUIRED", "واحد مالیاتی برگ اجرا مشخص نشده است");
        }
        Long taxAffairsOfficeId = taxUnitRepository.findTaxAffairsOfficeIdByTaxUnitId(taxUnitId)
                .orElseThrow(() -> new BusinessException("TAX_OFFICE_NOT_FOUND_FOR_UNIT", "اداره مالیاتی واحد برگ اجرا یافت نشد"));
        Long generalDirectorateId = taxUnitRepository.findGeneralDirectorateIdByTaxUnitId(taxUnitId)
                .orElseThrow(() -> new BusinessException("GENERAL_OFFICE_NOT_FOUND_FOR_UNIT", "اداره کل مالیاتی واحد برگ اجرا یافت نشد"));

        variables.put(ProcessConstants.VAR_TAX_AFFAIRS_OFFICE_ID, taxAffairsOfficeId);
        variables.put(ProcessConstants.VAR_GENERAL_DIRECTORATE_ID, generalDirectorateId);
        variables.put(ProcessConstants.VAR_GENERAL_OFFICE_HEAD_CANDIDATE_GROUPS,
                List.of(OrganizationCandidateGroup.forOrganization(BusinessRole.GENERAL_OFFICE_HEAD, generalDirectorateId)));
        variables.put(ProcessConstants.VAR_TAX_OFFICE_HEAD_CANDIDATE_GROUPS,
                List.of(OrganizationCandidateGroup.forOrganization(BusinessRole.TAX_OFFICE_HEAD, taxAffairsOfficeId)));
        variables.put(ProcessConstants.VAR_TAX_UNIT_HEAD_CANDIDATE_GROUPS,
                List.of(OrganizationCandidateGroup.forOrganization(BusinessRole.TAX_UNIT_HEAD, taxUnitId)));
        variables.put(ProcessConstants.VAR_TAX_EXPERT_CANDIDATE_GROUPS,
                List.of(OrganizationCandidateGroup.forOrganization(BusinessRole.TAX_EXPERT, taxUnitId)));
    }
}
