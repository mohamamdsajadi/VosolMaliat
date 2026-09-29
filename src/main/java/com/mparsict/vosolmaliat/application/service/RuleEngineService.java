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
        SanimModelValidation.requireDocument(request.sourceDocument());
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
        var d = request.sourceDocument();
        return switch (field == null ? "" : field) {
            case "performanceYear", "performanceYearPeriod", "taxYear" -> d.getTaxYear();
            case "performancePeriod", "taxPeriod" -> d.getTaxPeriod();
            case "mainTaxBureauId" -> request.mainTaxBureauId();
            case "taxBureauId" -> request.taxBureauId();
            case "taxAffairsOfficeId" -> request.taxBureauId();
            case "taxOffice" -> request.taxBureauId() != null ? request.taxBureauId() : request.mainTaxBureauId();
            case "taxSource" -> request.taxSource();
            case "sourceTaxOfficeCode" -> d.getSourceTaxOfficeCode();
            case "activityTitle" -> d.getActivityTitle();
            case "taxpayerType" -> request.taxpayer() == null ? null : request.taxpayer().type();
            case "issuanceReason" -> d.getIssuanceReason();
            default -> SanimModelValidation.amount(field, d.getAmounts());
        };
    }
}
