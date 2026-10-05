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
import com.mparsict.vosolmaliat.domain.model.execution.FinalTaxDebtPaper;
import com.mparsict.vosolmaliat.domain.model.rule.ExecutionPaperIssuanceRequest;
import com.mparsict.vosolmaliat.domain.repository.DebtInformationRepository;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperRepository;
import com.mparsict.vosolmaliat.domain.repository.FinalTaxDebtPaperRepository;
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
    private final FinalTaxDebtPaperRepository finalTaxDebtPaperRepository;
    private final TaxpayerRepository taxpayerRepository;
    private final ExecutionPaperRepository executionPaperRepository;
    private final DebtInformationRepository debtInformationRepository;
    private final UniqueCodeGenerator uniqueCodeGenerator;

    @Transactional
    public ExecutionPaperDetailResponse createFinalTaxDebtPaperAndExecutionPaper(DebtPaperIssuanceRequest request) {
        ExecutionPaper executionPaper = executionPaperService.createFromRequest(request);
        return executionPaperService.getDetails(executionPaper.getId());
    }

    @Transactional
    public IssuanceResponse sendFinalTaxDebtPaperToWorkflow(DebtPaperIssuanceRequest request) {
        try {
            ExecutionPaperIssuanceRequest savedRequest = issuanceRequestService.createAndSave(request);
            return camundaProcessService.startExecutionPaperIssuanceProcess(savedRequest.getId(), request);
        } catch (Exception ex) {
            throw new BusinessException("MANUAL_FINALIZED_DEBT_SEND_FAILED", "ارسال دستی برگ بدهی قطعی ناموفق بود", ex);
        }
    }

    @Transactional
    public ExecutionPaperDetailResponse createManualExecutionPaper(ManualExecutionPaperRequest request) {
        uniqueCodeGenerator.lockIssuance();
        if (blankToNull(request.executionPaperUniqueCode()) != null)
            throw new BusinessException("GENERATED_NUMBER_REQUIRED", "شماره برگ اجرا فقط توسط سامانه تخصیص می‌یابد");
        FinalTaxDebtPaper finalTaxDebtPaper = finalTaxDebtPaperRepository.findById(request.finalTaxDebtPaperId())
                .orElseThrow(() -> new BusinessException("FINALIZED_DEBT_PAPER_NOT_FOUND", "برگ بدهی قطعی یافت نشد"));
        Taxpayer taxpayer = taxpayerRepository.findById(request.taxpayerId())
                .orElseThrow(() -> new BusinessException("TAXPAYER_NOT_FOUND", "مودی یافت نشد"));

        if (finalTaxDebtPaper.getPaperType() == com.mparsict.vosolmaliat.domain.enums.PaperType.EF03)
            throw new BusinessException("EF03_RECEIPT_REQUIRED", "برای EF03 از مسیر دریافت و تأیید هویت استفاده کنید");
        var number = uniqueCodeGenerator.generateExecutionPaperNumber();
        ExecutionPaper saved = executionPaperRepository.save(ExecutionPaper.builder()
                .executionPaperUniqueCode(number.code())
                .issuanceDate(java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Tehran")))
                .finalTaxDebtPaper(finalTaxDebtPaper)
                .currentState(blankToNull(request.currentState()) == null
                        ? ExecutionPaperState.UNASSIGNED.name()
                        : request.currentState())
                .build());

        com.mparsict.vosolmaliat.application.service.SanimModelValidation.requireAmounts(request.currentAmounts());
        debtInformationRepository.save(DebtInformation.builder().executionPaper(saved).finalTaxDebtPaper(finalTaxDebtPaper)
                .currentAmounts(request.currentAmounts().copy()).build());
        return executionPaperService.getDetails(saved.getId());
    }

    @Transactional(readOnly = true)
    public AdminDataSummaryResponse summary() {
        return new AdminDataSummaryResponse(
                finalTaxDebtPaperRepository.findAll().stream()
                        .map(item -> new AdminDataSummaryResponse.FinalTaxDebtPaperSummary(
                                item.getId(),
                                item.getUniqueFinalTaxDebtNoticeId(),
                                item.getTaxSource().name()
                        ))
                        .toList(),
                taxpayerRepository.findAll().stream()
                        .map(item -> new AdminDataSummaryResponse.TaxpayerSummary(
                                item.getId(),
                                item.getTin(),
                                item.getRegistrationTraceCode(),
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

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
