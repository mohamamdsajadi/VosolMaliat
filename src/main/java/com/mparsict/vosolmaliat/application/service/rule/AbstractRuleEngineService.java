package com.mparsict.vosolmaliat.application.service.rule;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@Slf4j
public abstract class AbstractRuleEngineService<R, C> {

    private final ObjectMapper objectMapper;

    protected AbstractRuleEngineService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    protected GenericRuleEvaluationResult evaluateRules(
            List<R> rules,
            C context,
            Function<R, Long> ruleIdResolver,
            Function<R, String> ruleJsonResolver,
            Function<R, String> ruleEndDateResolver,
            String ruleTypeLabel) {
        List<String> rejectionCodes = new ArrayList<>();
        List<String> rejectionDescriptions = new ArrayList<>();
        List<String> endDates = new ArrayList<>();

        for (R rule : rules) {
            try {
                RuleDefinition definition = parseRuleDefinition(ruleJsonResolver.apply(rule), ruleTypeLabel);
                if (definition == null || definition.conditions() == null || definition.conditions().isEmpty()) {
                    continue;
                }
                if (evaluateRule(definition, context)) {
                    rejectionCodes.add(definition.rejectionReasonCode());
                    rejectionDescriptions.add(definition.rejectionReasonDescription());
                    String endDate = ruleEndDateResolver.apply(rule);
                    if (endDate != null) {
                        endDates.add(endDate);
                    }
                }
            } catch (com.mparsict.vosolmaliat.common.exception.DebtPaperPushException ex) {
                throw ex;
            } catch (Exception ex) {
                log.error("Error evaluating {} rule id={}: {}", ruleTypeLabel, ruleIdResolver.apply(rule), ex.getMessage());
            }
        }

        if (rejectionCodes.isEmpty()) {
            return GenericRuleEvaluationResult.success();
        }

        return new GenericRuleEvaluationResult(
                false,
                rejectionCodes.getFirst(),
                rejectionDescriptions.getFirst(),
                endDates.stream().max(String::compareTo).orElse(null)
        );
    }

    private RuleDefinition parseRuleDefinition(String ruleJson, String ruleTypeLabel) {
        try {
            Map<String, Object> map = objectMapper.readerFor(new TypeReference<Map<String, Object>>() {})
                    .with(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).readValue(ruleJson);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> conditionMaps = (List<Map<String, Object>>) map.get("conditions");
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
            log.error("Failed to parse {} rule JSON: {}", ruleTypeLabel, ex.getMessage());
            return null;
        }
    }

    private boolean evaluateRule(RuleDefinition definition, C context) {
        boolean isAnd = !"OR".equalsIgnoreCase(definition.logicalOperator());
        for (Condition condition : definition.conditions()) {
            boolean result = evaluateCondition(condition, context);
            if (isAnd && !result) {
                return false;
            }
            if (!isAnd && result) {
                return true;
            }
        }
        return isAnd;
    }

    private boolean evaluateCondition(Condition condition, C context) {
        Object fieldValue = resolveField(condition.field(), context);
        Object conditionValue = condition.value();
        if (fieldValue == null || conditionValue == null || condition.operator() == null) {
            return false;
        }

        return switch (condition.operator().toUpperCase()) {
            case "EQUALS" -> fieldValue instanceof Number ? compareNumbers(fieldValue, conditionValue) == 0
                    : fieldValue.toString().equalsIgnoreCase(conditionValue.toString());
            case "NOT_EQUALS" -> fieldValue instanceof Number ? compareNumbers(fieldValue, conditionValue) != 0
                    : !fieldValue.toString().equalsIgnoreCase(conditionValue.toString());
            case "GREATER_THAN" -> compareNumbers(fieldValue, conditionValue) > 0;
            case "LESS_THAN" -> compareNumbers(fieldValue, conditionValue) < 0;
            case "GREATER_THAN_OR_EQUAL" -> compareNumbers(fieldValue, conditionValue) >= 0;
            case "LESS_THAN_OR_EQUAL" -> compareNumbers(fieldValue, conditionValue) <= 0;
            case "IN" -> toList(conditionValue).stream()
                    .filter(Objects::nonNull)
                    .anyMatch(value -> value.toString().equalsIgnoreCase(fieldValue.toString()));
            case "NOT_IN" -> toList(conditionValue).stream()
                    .filter(Objects::nonNull)
                    .noneMatch(value -> value.toString().equalsIgnoreCase(fieldValue.toString()));
            default -> {
                log.warn("Unknown {} rule operator: {}", ruleTypeLabel(), condition.operator());
                yield false;
            }
        };
    }

    protected abstract Object resolveField(String field, C context);

    protected String ruleTypeLabel() {
        return getClass().getSimpleName();
    }

    private int compareNumbers(Object left, Object right) {
        return new java.math.BigDecimal(left.toString()).compareTo(new java.math.BigDecimal(right.toString()));
    }

    protected double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    protected List<?> toList(Object value) {
        if (value instanceof List<?> list) {
            return list;
        }
        return List.of(value);
    }

    protected record GenericRuleEvaluationResult(
            boolean allowed,
            String rejectionReasonCode,
            String rejectionReasonDescription,
            String restrictionRemovalDate
    ) {
        private static GenericRuleEvaluationResult success() {
            return new GenericRuleEvaluationResult(true, null, null, null);
        }
    }

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
}
