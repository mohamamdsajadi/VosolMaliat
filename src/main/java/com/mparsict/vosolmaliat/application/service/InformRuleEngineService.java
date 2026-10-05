package com.mparsict.vosolmaliat.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.common.exception.BusinessException;
import com.mparsict.vosolmaliat.domain.model.execution.DebtInformation;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaper;
import com.mparsict.vosolmaliat.domain.model.execution.FinalTaxDebtPaper;
import com.mparsict.vosolmaliat.domain.model.rule.ExecutionPaperInformRejectionRule;
import com.mparsict.vosolmaliat.application.service.rule.AbstractRuleEngineService;
import com.mparsict.vosolmaliat.domain.repository.DebtInformationRepository;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperInformRejectionRuleRepository;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperRepository;
import com.mparsict.vosolmaliat.domain.repository.FinalTaxDebtPaperRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class InformRuleEngineService extends AbstractRuleEngineService<ExecutionPaperInformRejectionRule, InformRuleEngineService.InformRuleContext> {

    private final ExecutionPaperInformRejectionRuleRepository ruleRepository;
    private final ExecutionPaperRepository executionPaperRepository;
    private final FinalTaxDebtPaperRepository finalTaxDebtPaperRepository;
    private final DebtInformationRepository debtInformationRepository;

    public InformRuleEngineService(
            ExecutionPaperInformRejectionRuleRepository ruleRepository,
            ExecutionPaperRepository executionPaperRepository,
            FinalTaxDebtPaperRepository finalTaxDebtPaperRepository,
            DebtInformationRepository debtInformationRepository,
            ObjectMapper objectMapper) {
        super(objectMapper);
        this.ruleRepository = ruleRepository;
        this.executionPaperRepository = executionPaperRepository;
        this.finalTaxDebtPaperRepository = finalTaxDebtPaperRepository;
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
            FinalTaxDebtPaper finalTaxDebtPaper,
            DebtInformation debtInformation
    ) {}

    public InformRuleEvaluationResult evaluate(Long executionPaperId) {
        ExecutionPaper executionPaper = executionPaperRepository.findById(executionPaperId)
                .orElseThrow(() -> new BusinessException("EXECUTION_PAPER_NOT_FOUND", "برگ اجرا یافت نشد"));
        FinalTaxDebtPaper finalTaxDebtPaper = executionPaper.getFinalTaxDebtPaperId() == null
                ? null
                : finalTaxDebtPaperRepository.findById(executionPaper.getFinalTaxDebtPaperId()).orElse(null);
        DebtInformation debtInformation = debtInformationRepository.findByExecutionPaperId(executionPaperId).orElse(null);

        if (finalTaxDebtPaper == null) {
            throw new BusinessException("FINAL_TAX_DEBT_PAPER_NOT_FOUND", "برگ بدهی قطعی یافت نشد");
        }
        SanimModelValidation.requireAmounts(debtInformation == null ? null : debtInformation.getCurrentAmounts());
        String today = LocalDate.now().toString();
        List<ExecutionPaperInformRejectionRule> activeRules = ruleRepository.findActiveRules(today);
        GenericRuleEvaluationResult result = evaluateRules(
                activeRules,
                new InformRuleContext(executionPaper, finalTaxDebtPaper, debtInformation),
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
                context.finalTaxDebtPaper(),
                context.debtInformation()
        );
    }

    private Object resolveField(
            String field,
            ExecutionPaper executionPaper,
            FinalTaxDebtPaper finalTaxDebtPaper,
            DebtInformation debtInformation) {
        var office = finalTaxDebtPaper.getTaxAffairsOffice();
        return switch (field == null ? "" : field) {
            case "performanceYear", "performanceYearPeriod", "taxYear" -> finalTaxDebtPaper.getTaxYear();
            case "performancePeriod", "taxPeriod" -> finalTaxDebtPaper.getTaxPeriod();
            case "mainTaxBureauId" -> office == null ? null : office.getGeneralDirectorateOfTaxAffairsId();
            case "taxBureauId" -> finalTaxDebtPaper.getTaxAffairsOfficeId();
            case "taxAffairsOfficeId" -> executionPaper.getFinalTaxDebtPaper() == null
                    ? null : executionPaper.getFinalTaxDebtPaper().getTaxAffairsOfficeId();
            case "taxSource" -> finalTaxDebtPaper.getTaxSource();
            case "sourceTaxOfficeCode" -> office == null ? null : office.getOfficeCode();
            case "activityTitle" -> finalTaxDebtPaper.getTaxpayer().getActivityType() == null ? null
                    : finalTaxDebtPaper.getTaxpayer().getActivityType().getActivityTypeTitle();
            case "issuanceReason" -> finalTaxDebtPaper.getFinalizationReason();
            default -> SanimModelValidation.amount(field, debtInformation.getCurrentAmounts());
        };
    }
}
