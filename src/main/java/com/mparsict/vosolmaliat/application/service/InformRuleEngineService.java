package com.mparsict.vosolmaliat.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.common.exception.BusinessException;
import com.mparsict.vosolmaliat.domain.model.execution.DebtInformation;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaper;
import com.mparsict.vosolmaliat.domain.model.execution.FinalizedDebtPaper;
import com.mparsict.vosolmaliat.domain.model.rule.ExecutionPaperInformRejectionRule;
import com.mparsict.vosolmaliat.domain.repository.DebtInformationRepository;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperInformRejectionRuleRepository;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperRepository;
import com.mparsict.vosolmaliat.domain.repository.FinalizedDebtPaperRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class InformRuleEngineService {

    private final ExecutionPaperInformRejectionRuleRepository ruleRepository;
    private final ExecutionPaperRepository executionPaperRepository;
    private final FinalizedDebtPaperRepository finalizedDebtPaperRepository;
    private final DebtInformationRepository debtInformationRepository;
    private final ObjectMapper objectMapper;

    public record InformRuleEvaluationResult(
            boolean canInform,
            String rejectionReasonCode,
            String rejectionReasonDescription,
            String restrictionRemovalDate
    ) {}

    private record RuleDefinition(
            List<Condition> conditions,
            String logicalOperator,
            String rejectionReasonCode,
            String rejectionReasonDescription
    ) {}

    private record Condition(String field, String operator, Object value) {}

    public InformRuleEvaluationResult evaluate(Long executionPaperId) {
        ExecutionPaper executionPaper = executionPaperRepository.findById(executionPaperId)
                .orElseThrow(() -> new BusinessException("EXECUTION_PAPER_NOT_FOUND", "برگ اجرا یافت نشد"));
        FinalizedDebtPaper finalizedDebtPaper = executionPaper.getFinalizedDebtPaperId() == null
                ? null
                : finalizedDebtPaperRepository.findById(executionPaper.getFinalizedDebtPaperId()).orElse(null);
        DebtInformation debtInformation = debtInformationRepository.findByExecutionPaperId(executionPaperId).orElse(null);

        String today = LocalDate.now().toString();
        List<ExecutionPaperInformRejectionRule> activeRules = ruleRepository.findActiveRules(today);
        List<String> rejectionCodes = new ArrayList<>();
        List<String> rejectionDescriptions = new ArrayList<>();
        List<String> endDates = new ArrayList<>();

        for (ExecutionPaperInformRejectionRule rule : activeRules) {
            try {
                RuleDefinition definition = parseRuleDefinition(rule.getRuleJson());
                if (definition == null || definition.conditions() == null || definition.conditions().isEmpty()) {
                    continue;
                }
                if (evaluateRule(definition, executionPaper, finalizedDebtPaper, debtInformation)) {
                    rejectionCodes.add(definition.rejectionReasonCode());
                    rejectionDescriptions.add(definition.rejectionReasonDescription());
                    endDates.add(rule.getEndDate());
                }
            } catch (Exception ex) {
                log.error("Error evaluating inform rule id={}: {}", rule.getId(), ex.getMessage());
            }
        }

        if (rejectionCodes.isEmpty()) {
            return new InformRuleEvaluationResult(true, null, null, null);
        }

        String latestEndDate = endDates.stream().max(String::compareTo).orElse(null);
        return new InformRuleEvaluationResult(false, rejectionCodes.get(0), rejectionDescriptions.get(0), latestEndDate);
    }

    private RuleDefinition parseRuleDefinition(String ruleJson) {
        try {
            Map<String, Object> map = objectMapper.readValue(ruleJson, new TypeReference<>() {});
            List<Map<String, Object>> conditionMaps = objectMapper.convertValue(map.get("conditions"), new TypeReference<>() {});
            List<Condition> conditions = new ArrayList<>();
            if (conditionMaps != null) {
                for (Map<String, Object> condition : conditionMaps) {
                    conditions.add(new Condition(
                            (String) condition.get("field"),
                            (String) condition.get("operator"),
                            condition.get("value")
                    ));
                }
            }
            return new RuleDefinition(
                    conditions,
                    (String) map.getOrDefault("logicalOperator", "AND"),
                    (String) map.get("rejectionReasonCode"),
                    (String) map.get("rejectionReasonDescription")
            );
        } catch (Exception ex) {
            log.error("Failed to parse inform rule JSON: {}", ex.getMessage());
            return null;
        }
    }

    private boolean evaluateRule(
            RuleDefinition definition,
            ExecutionPaper executionPaper,
            FinalizedDebtPaper finalizedDebtPaper,
            DebtInformation debtInformation) {
        boolean isAnd = !"OR".equalsIgnoreCase(definition.logicalOperator());
        for (Condition condition : definition.conditions()) {
            boolean result = evaluateCondition(condition, executionPaper, finalizedDebtPaper, debtInformation);
            if (isAnd && !result) {
                return false;
            }
            if (!isAnd && result) {
                return true;
            }
        }
        return isAnd;
    }

    private boolean evaluateCondition(
            Condition condition,
            ExecutionPaper executionPaper,
            FinalizedDebtPaper finalizedDebtPaper,
            DebtInformation debtInformation) {
        Object fieldValue = resolveField(condition.field(), executionPaper, finalizedDebtPaper, debtInformation);
        Object conditionValue = condition.value();
        if (fieldValue == null || conditionValue == null || condition.operator() == null) {
            return false;
        }
        return switch (condition.operator().toUpperCase()) {
            case "EQUALS" -> fieldValue.toString().equalsIgnoreCase(conditionValue.toString());
            case "NOT_EQUALS" -> !fieldValue.toString().equalsIgnoreCase(conditionValue.toString());
            case "GREATER_THAN" -> toDouble(fieldValue) > toDouble(conditionValue);
            case "LESS_THAN" -> toDouble(fieldValue) < toDouble(conditionValue);
            case "GREATER_THAN_OR_EQUAL" -> toDouble(fieldValue) >= toDouble(conditionValue);
            case "LESS_THAN_OR_EQUAL" -> toDouble(fieldValue) <= toDouble(conditionValue);
            case "IN" -> toList(conditionValue).stream().anyMatch(v -> v.toString().equalsIgnoreCase(fieldValue.toString()));
            case "NOT_IN" -> toList(conditionValue).stream().noneMatch(v -> v.toString().equalsIgnoreCase(fieldValue.toString()));
            default -> false;
        };
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

    private double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    private List<?> toList(Object value) {
        if (value instanceof List<?> list) {
            return list;
        }
        return List.of(value);
    }
}
