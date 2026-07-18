package com.mparsict.vosolmaliat.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.common.exception.BusinessException;
import com.mparsict.vosolmaliat.domain.model.execution.DebtInformation;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaper;
import com.mparsict.vosolmaliat.domain.model.execution.FinalizedDebtPaper;
import com.mparsict.vosolmaliat.domain.model.rule.ExecutionPaperInformRejectionRule;
import com.mparsict.vosolmaliat.application.service.rule.AbstractRuleEngineService;
import com.mparsict.vosolmaliat.domain.repository.DebtInformationRepository;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperInformRejectionRuleRepository;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperRepository;
import com.mparsict.vosolmaliat.domain.repository.FinalizedDebtPaperRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class InformRuleEngineService extends AbstractRuleEngineService<ExecutionPaperInformRejectionRule, InformRuleEngineService.InformRuleContext> {

    private final ExecutionPaperInformRejectionRuleRepository ruleRepository;
    private final ExecutionPaperRepository executionPaperRepository;
    private final FinalizedDebtPaperRepository finalizedDebtPaperRepository;
    private final DebtInformationRepository debtInformationRepository;

    public InformRuleEngineService(
            ExecutionPaperInformRejectionRuleRepository ruleRepository,
            ExecutionPaperRepository executionPaperRepository,
            FinalizedDebtPaperRepository finalizedDebtPaperRepository,
            DebtInformationRepository debtInformationRepository,
            ObjectMapper objectMapper) {
        super(objectMapper);
        this.ruleRepository = ruleRepository;
        this.executionPaperRepository = executionPaperRepository;
        this.finalizedDebtPaperRepository = finalizedDebtPaperRepository;
        this.debtInformationRepository = debtInformationRepository;
    }

    public record InformRuleEvaluationResult(
            boolean canInform,
            String rejectionReasonCode,
            String rejectionReasonDescription,
            String restrictionRemovalDate
    ) {}

    protected record InformRuleContext(
            ExecutionPaper executionPaper,
            FinalizedDebtPaper finalizedDebtPaper,
            DebtInformation debtInformation
    ) {}

    public InformRuleEvaluationResult evaluate(Long executionPaperId) {
        ExecutionPaper executionPaper = executionPaperRepository.findById(executionPaperId)
                .orElseThrow(() -> new BusinessException("EXECUTION_PAPER_NOT_FOUND", "برگ اجرا یافت نشد"));
        FinalizedDebtPaper finalizedDebtPaper = executionPaper.getFinalizedDebtPaperId() == null
                ? null
                : finalizedDebtPaperRepository.findById(executionPaper.getFinalizedDebtPaperId()).orElse(null);
        DebtInformation debtInformation = debtInformationRepository.findByExecutionPaperId(executionPaperId).orElse(null);

        String today = LocalDate.now().toString();
        List<ExecutionPaperInformRejectionRule> activeRules = ruleRepository.findActiveRules(today);
        GenericRuleEvaluationResult result = evaluateRules(
                activeRules,
                new InformRuleContext(executionPaper, finalizedDebtPaper, debtInformation),
                ExecutionPaperInformRejectionRule::getId,
                ExecutionPaperInformRejectionRule::getRuleJson,
                ExecutionPaperInformRejectionRule::getEndDate,
                "inform"
        );
        return new InformRuleEvaluationResult(
                result.allowed(),
                result.rejectionReasonCode(),
                result.rejectionReasonDescription(),
                result.restrictionRemovalDate()
        );
    }

    @Override
    protected Object resolveField(String field, InformRuleContext context) {
        return resolveField(
                field,
                context.executionPaper(),
                context.finalizedDebtPaper(),
                context.debtInformation()
        );
    }

    private Object resolveField(
            String field,
            ExecutionPaper executionPaper,
            FinalizedDebtPaper finalizedDebtPaper,
            DebtInformation debtInformation) {
        return switch (field == null ? "" : field) {
            case "performanceYear", "performanceYearPeriod" -> finalizedDebtPaper == null ? null : finalizedDebtPaper.getPerformanceYear();
            case "performancePeriod" -> finalizedDebtPaper == null ? null : finalizedDebtPaper.getPerformancePeriod();
            case "mainTaxBureauId" -> finalizedDebtPaper == null ? null : finalizedDebtPaper.getMainTaxBureauId();
            case "taxBureauId" -> finalizedDebtPaper == null ? null : finalizedDebtPaper.getTaxBureauId();
            case "taxUnitId" -> executionPaper.getTaxUnitId();
            case "taxSource" -> executionPaper.getTaxSource();
            case "activityTypeCode" -> finalizedDebtPaper == null ? null : finalizedDebtPaper.getActivityTypeCode();
            case "totalFinalizedAmount" -> finalizedDebtPaper == null ? null : finalizedDebtPaper.getTotalFinalizedAmount();
            case "finalizedTaxAndNonDeductibleAmount" -> finalizedDebtPaper == null ? null : finalizedDebtPaper.getFinalizedTaxAndNonDeductibleAmount();
            case "debtAmount" -> debtInformation == null ? null : debtInformation.getTotalDebt();
            case "finalizationReason" -> executionPaper.getFinalizationReason();
            default -> null;
        };
    }
}
