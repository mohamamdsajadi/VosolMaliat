package com.mparsict.vosolmaliat.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.request.DebtPaperIssuanceRequest;
import com.mparsict.vosolmaliat.application.service.rule.AbstractRuleEngineService;
import com.mparsict.vosolmaliat.domain.model.rule.ExecutionPaperRejectionRule;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperRejectionRuleRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class RuleEngineService extends AbstractRuleEngineService<ExecutionPaperRejectionRule, DebtPaperIssuanceRequest> {

    private final ExecutionPaperRejectionRuleRepository ruleRepository;

    public RuleEngineService(
            ExecutionPaperRejectionRuleRepository ruleRepository,
            ObjectMapper objectMapper) {
        super(objectMapper);
        this.ruleRepository = ruleRepository;
    }

    public record RuleEvaluationResult(
            boolean canIssue,
            String rejectionReasonCode,
            String rejectionReasonDescription,
            String restrictionRemovalDate
    ) {}

    public RuleEvaluationResult evaluate(DebtPaperIssuanceRequest request) {
        String today = LocalDate.now().toString();
        List<ExecutionPaperRejectionRule> activeRules = ruleRepository.findActiveRules(today);

        GenericRuleEvaluationResult result = evaluateRules(
                activeRules,
                request,
                ExecutionPaperRejectionRule::getId,
                ExecutionPaperRejectionRule::getRuleJson,
                ExecutionPaperRejectionRule::getEndDate,
                "issuance"
        );
        return new RuleEvaluationResult(
                result.allowed(),
                result.rejectionReasonCode(),
                result.rejectionReasonDescription(),
                result.restrictionRemovalDate()
        );
    }

    @Override
    protected Object resolveField(String field, DebtPaperIssuanceRequest request) {
        return switch (field == null ? "" : field) {
            case "performanceYear", "performanceYearPeriod" -> request.performanceYear();
            case "performancePeriod" -> request.performancePeriod();
            case "mainTaxBureauId" -> request.mainTaxBureauId();
            case "taxBureauId" -> request.taxBureauId();
            case "taxOffice" -> request.taxBureauId() != null ? request.taxBureauId() : request.mainTaxBureauId();
            case "taxSource" -> request.taxSource();
            case "activityTypeCode" -> request.activityTypeCode();
            case "totalFinalizedAmount" -> computeTotalFinalizedAmount(request);
            case "finalizedTaxAndNonDeductibleAmount" -> computeFinalizedTaxAndNonDeductibleAmount(request);
            case "debtAmount" -> computeTotalDebtAmount(request);
            case "taxpayerType" -> request.taxpayer() != null ? request.taxpayer().type() : null;
            case "finalizationReason" -> request.finalizationReason();
            default -> null;
        };
    }

    private double computeTotalFinalizedAmount(DebtPaperIssuanceRequest request) {
        if (request.debtInfo() == null) return 0.0;
        DebtPaperIssuanceRequest.DebtInfo d = request.debtInfo();
        return orZero(d.taxAmount()) + orZero(d.penaltyArticle22()) + orZero(d.nonDeductiblePenalty());
    }

    private double computeFinalizedTaxAndNonDeductibleAmount(DebtPaperIssuanceRequest request) {
        if (request.debtInfo() == null) return 0.0;
        DebtPaperIssuanceRequest.DebtInfo d = request.debtInfo();
        return orZero(d.taxAmount()) + orZero(d.nonDeductiblePenalty());
    }

    private double computeTotalDebtAmount(DebtPaperIssuanceRequest request) {
        if (request.debtInfo() == null) return 0.0;
        DebtPaperIssuanceRequest.DebtInfo d = request.debtInfo();
        return orZero(d.taxAmount()) + orZero(d.dutyAmount())
                + orZero(d.penaltyArticle22()) + orZero(d.penaltyArticle23())
                + orZero(d.deductiblePenalty()) + orZero(d.nonDeductiblePenalty())
                - orZero(d.taxPaidAmount()) - orZero(d.penaltyPaidAmount());
    }

    private double orZero(Double val) {
        return val != null ? val : 0.0;
    }
}
