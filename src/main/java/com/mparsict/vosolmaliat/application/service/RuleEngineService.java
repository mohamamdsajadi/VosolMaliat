package com.mparsict.vosolmaliat.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.request.DebtPaperIssuanceRequest;
import com.mparsict.vosolmaliat.domain.model.rule.ExecutionPaperRejectionRule;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperRejectionRuleRepository;
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
public class RuleEngineService {

    private final ExecutionPaperRejectionRuleRepository ruleRepository;
    private final ObjectMapper objectMapper;

    public record RuleEvaluationResult(
            boolean canIssue,
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

    private record Condition(
            String field,
            String operator,
            Object value
    ) {}

    public RuleEvaluationResult evaluate(DebtPaperIssuanceRequest request) {
        String today = LocalDate.now().toString();
        List<ExecutionPaperRejectionRule> activeRules = ruleRepository.findActiveRules(today);

        List<String> rejectionCodes = new ArrayList<>();
        List<String> rejectionDescs = new ArrayList<>();
        List<String> endDates = new ArrayList<>();

        for (ExecutionPaperRejectionRule rule : activeRules) {
            try {
                RuleDefinition definition = parseRuleDefinition(rule.getRuleJson());
                if (definition == null) continue;

                boolean isBlocked = evaluateRule(definition, request);
                if (isBlocked) {
                    rejectionCodes.add(definition.rejectionReasonCode());
                    rejectionDescs.add(definition.rejectionReasonDescription());
                    endDates.add(rule.getEndDate());
                }
            } catch (Exception e) {
                log.error("Error evaluating rule id={}: {}", rule.getId(), e.getMessage());
            }
        }

        if (rejectionCodes.isEmpty()) {
            return new RuleEvaluationResult(true, null, null, null);
        }

        // Return the latest end date among all blocking rules
        String latestEndDate = endDates.stream()
                .max(String::compareTo)
                .orElse(null);

        return new RuleEvaluationResult(
                false,
                rejectionCodes.get(0),
                rejectionDescs.get(0),
                latestEndDate
        );
    }

    private RuleDefinition parseRuleDefinition(String ruleJson) {
        try {
            Map<String, Object> map = objectMapper.readValue(ruleJson, new TypeReference<>() {});
            String logicalOperator = (String) map.getOrDefault("logicalOperator", "AND");
            String code = (String) map.get("rejectionReasonCode");
            String desc = (String) map.get("rejectionReasonDescription");

            List<Map<String, Object>> conditionMaps = objectMapper.convertValue(
                    map.get("conditions"), new TypeReference<>() {});

            List<Condition> conditions = new ArrayList<>();
            if (conditionMaps != null) {
                for (Map<String, Object> cm : conditionMaps) {
                    conditions.add(new Condition(
                            (String) cm.get("field"),
                            (String) cm.get("operator"),
                            cm.get("value")
                    ));
                }
            }
            return new RuleDefinition(conditions, logicalOperator, code, desc);
        } catch (Exception e) {
            log.error("Failed to parse rule JSON: {}", e.getMessage());
            return null;
        }
    }

    private boolean evaluateRule(RuleDefinition definition, DebtPaperIssuanceRequest request) {
        List<Condition> conditions = definition.conditions();
        if (conditions == null || conditions.isEmpty()) return false;

        boolean isAnd = !"OR".equalsIgnoreCase(definition.logicalOperator());

        for (Condition condition : conditions) {
            boolean condResult = evaluateCondition(condition, request);
            if (isAnd && !condResult) return false;
            if (!isAnd && condResult) return true;
        }

        // AND: all passed → blocked; OR: none passed → not blocked
        return isAnd;
    }

    private boolean evaluateCondition(Condition condition, DebtPaperIssuanceRequest request) {
        Object fieldValue = resolveField(condition.field(), request);
        Object condValue = condition.value();

        if (fieldValue == null || condValue == null) return false;

        return switch (condition.operator().toUpperCase()) {
            case "EQUALS" -> fieldValue.toString().equalsIgnoreCase(condValue.toString());
            case "NOT_EQUALS" -> !fieldValue.toString().equalsIgnoreCase(condValue.toString());
            case "GREATER_THAN" -> toDouble(fieldValue) > toDouble(condValue);
            case "LESS_THAN" -> toDouble(fieldValue) < toDouble(condValue);
            case "GREATER_THAN_OR_EQUAL" -> toDouble(fieldValue) >= toDouble(condValue);
            case "LESS_THAN_OR_EQUAL" -> toDouble(fieldValue) <= toDouble(condValue);
            case "IN" -> {
                List<?> list = toList(condValue);
                yield list.stream().anyMatch(v -> v.toString().equalsIgnoreCase(fieldValue.toString()));
            }
            case "NOT_IN" -> {
                List<?> list = toList(condValue);
                yield list.stream().noneMatch(v -> v.toString().equalsIgnoreCase(fieldValue.toString()));
            }
            default -> {
                log.warn("Unknown operator: {}", condition.operator());
                yield false;
            }
        };
    }

    private Object resolveField(String field, DebtPaperIssuanceRequest request) {
        return switch (field) {
            case "debtAmount" -> computeTotalDebtAmount(request);
            case "taxpayerType" -> request.taxpayer() != null ? request.taxpayer().type() : null;
            case "taxSource" -> request.taxSource();
            case "finalizationReason" -> request.finalizationReason();
            default -> null;
        };
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

    private double toDouble(Object val) {
        if (val instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(val.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    @SuppressWarnings("unchecked")
    private List<?> toList(Object val) {
        if (val instanceof List<?> list) return list;
        return List.of(val);
    }
}
