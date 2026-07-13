package com.mparsict.vosolmaliat.application.service.admin;

import com.mparsict.vosolmaliat.application.dto.request.DebtPaperIssuanceRequest;
import com.mparsict.vosolmaliat.application.dto.request.admin.ManualExecutionPaperRequest;
import com.mparsict.vosolmaliat.application.dto.response.ExecutionPaperDetailResponse;
import com.mparsict.vosolmaliat.application.dto.response.IssuanceResponse;
import com.mparsict.vosolmaliat.application.dto.response.admin.AdminDataSummaryResponse;
import com.mparsict.vosolmaliat.application.service.ExecutionPaperService;
import com.mparsict.vosolmaliat.application.service.IssuanceRequestService;
import com.mparsict.vosolmaliat.common.exception.BusinessException;
import com.mparsict.vosolmaliat.common.util.UniqueCodeGenerator;
import com.mparsict.vosolmaliat.domain.enums.ExecutionPaperState;
import com.mparsict.vosolmaliat.domain.model.Taxpayer;
import com.mparsict.vosolmaliat.domain.model.execution.DebtInformation;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaper;
import com.mparsict.vosolmaliat.domain.model.execution.FinalizedDebtPaper;
import com.mparsict.vosolmaliat.domain.model.masterdata.TaxUnit;
import com.mparsict.vosolmaliat.domain.model.rule.ExecutionPaperIssuanceRequest;
import com.mparsict.vosolmaliat.domain.repository.DebtInformationRepository;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperRepository;
import com.mparsict.vosolmaliat.domain.repository.FinalizedDebtPaperRepository;
import com.mparsict.vosolmaliat.domain.repository.TaxpayerRepository;
import com.mparsict.vosolmaliat.infrastructure.camunda.service.CamundaProcessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminDataEntryService {

    private final ExecutionPaperService executionPaperService;
    private final IssuanceRequestService issuanceRequestService;
    private final CamundaProcessService camundaProcessService;
    private final FinalizedDebtPaperRepository finalizedDebtPaperRepository;
    private final TaxpayerRepository taxpayerRepository;
    private final ExecutionPaperRepository executionPaperRepository;
    private final DebtInformationRepository debtInformationRepository;
    private final UniqueCodeGenerator uniqueCodeGenerator;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Transactional
    public ExecutionPaperDetailResponse createFinalizedDebtPaperAndExecutionPaper(DebtPaperIssuanceRequest request) {
        ExecutionPaper executionPaper = executionPaperService.createFromRequest(request);
        return executionPaperService.getDetails(executionPaper.getId());
    }

    @Transactional
    public IssuanceResponse sendFinalizedDebtPaperToWorkflow(DebtPaperIssuanceRequest request) {
        try {
            ExecutionPaperIssuanceRequest savedRequest = issuanceRequestService.createAndSave(request);
            String payload = objectMapper.writeValueAsString(request);
            return camundaProcessService.startExecutionPaperIssuanceProcess(savedRequest.getId(), payload);
        } catch (Exception ex) {
            throw new BusinessException("MANUAL_FINALIZED_DEBT_SEND_FAILED", "ارسال دستی برگ بدهی قطعی ناموفق بود", ex);
        }
    }

    @Transactional
    public ExecutionPaperDetailResponse createManualExecutionPaper(ManualExecutionPaperRequest request) {
        FinalizedDebtPaper finalizedDebtPaper = finalizedDebtPaperRepository.findById(request.finalizedDebtPaperId())
                .orElseThrow(() -> new BusinessException("FINALIZED_DEBT_PAPER_NOT_FOUND", "برگ بدهی قطعی یافت نشد"));
        Taxpayer taxpayer = taxpayerRepository.findById(request.taxpayerId())
                .orElseThrow(() -> new BusinessException("TAXPAYER_NOT_FOUND", "مودی یافت نشد"));

        ExecutionPaper saved = executionPaperRepository.save(ExecutionPaper.builder()
                .executionPaperUniqueCode(blankToNull(request.executionPaperUniqueCode()) == null
                        ? uniqueCodeGenerator.generateExecutionPaperCode()
                        : request.executionPaperUniqueCode())
                .finalizedDebtPaper(finalizedDebtPaper)
                .taxpayer(taxpayer)
                .taxUnit(request.taxUnitId() == null ? null : TaxUnit.builder().id(request.taxUnitId()).build())
                .finalizationReason(request.finalizationReason())
                .taxSource(request.taxSource())
                .currentState(blankToNull(request.currentState()) == null
                        ? ExecutionPaperState.UNASSIGNED.name()
                        : request.currentState())
                .build());

        if (request.debtInfo() != null) {
            debtInformationRepository.save(toDebtInformation(saved.getId(), request.debtInfo()));
        }
        return executionPaperService.getDetails(saved.getId());
    }

    @Transactional(readOnly = true)
    public AdminDataSummaryResponse summary() {
        return new AdminDataSummaryResponse(
                finalizedDebtPaperRepository.findAll().stream()
                        .map(item -> new AdminDataSummaryResponse.FinalizedDebtPaperSummary(
                                item.getId(),
                                item.getDebtPaperUniqueCode(),
                                item.getTaxSource()
                        ))
                        .toList(),
                taxpayerRepository.findAll().stream()
                        .map(item -> new AdminDataSummaryResponse.TaxpayerSummary(
                                item.getId(),
                                item.getDebtPaperUniqueCode(),
                                item.getDebtPaperIssueDate(),
                                item.getType().name()
                        ))
                        .toList(),
                executionPaperRepository.findAll().stream()
                        .map(item -> new AdminDataSummaryResponse.ExecutionPaperSummary(
                                item.getId(),
                                item.getExecutionPaperUniqueCode(),
                                item.getCurrentState()
                        ))
                        .toList()
        );
    }

    private DebtInformation toDebtInformation(Long executionPaperId, ManualExecutionPaperRequest.DebtInformationInput input) {
        return DebtInformation.builder()
                .executionPaper(ExecutionPaper.builder().id(executionPaperId).build())
                .taxAmount(orZero(input.taxAmount()))
                .dutyAmount(orZero(input.dutyAmount()))
                .penaltyArticle22(orZero(input.penaltyArticle22()))
                .penaltyArticle23(orZero(input.penaltyArticle23()))
                .deductiblePenalty(orZero(input.deductiblePenalty()))
                .nonDeductiblePenalty(orZero(input.nonDeductiblePenalty()))
                .taxPaidAmount(orZero(input.taxPaidAmount()))
                .penaltyPaidAmount(orZero(input.penaltyPaidAmount()))
                .taxCredit(orZero(input.taxCredit()))
                .remainingTax(orZero(input.remainingTax()))
                .municipalityShare(orZero(input.municipalityShare()))
                .guildShare(orZero(input.guildShare()))
                .complianceBonus(orZero(input.complianceBonus()))
                .build();
    }

    private Double orZero(Double value) {
        return value == null ? 0.0 : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
