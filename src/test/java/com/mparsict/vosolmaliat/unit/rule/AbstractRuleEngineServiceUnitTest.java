package com.mparsict.vosolmaliat.unit.rule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.service.rule.AbstractRuleEngineService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractRuleEngineServiceUnitTest {

    private final TestRuleEngine engine = new TestRuleEngine();

    @Test
    void andRuleBlocksWhenAllConditionsMatch() {
        TestRule rule = new TestRule(1L, """
                {
                  "logicalOperator": "AND",
                  "rejectionReasonCode": "AMOUNT_BLOCKED",
                  "rejectionReasonDescription": "Amount is too high",
                  "conditions": [
                    { "field": "taxSource", "operator": "EQUALS", "value": "VAT" },
                    { "field": "amount", "operator": "GREATER_THAN", "value": 1000 }
                  ]
                }
                """, "2026-08-01");

        TestEvaluationResult result = engine.evaluate(List.of(rule), Map.of("taxSource", "VAT", "amount", 1500));

        assertThat(result.allowed()).isFalse();
        assertThat(result.rejectionReasonCode()).isEqualTo("AMOUNT_BLOCKED");
        assertThat(result.restrictionRemovalDate()).isEqualTo("2026-08-01");
    }

    @Test
    void andRuleAllowsWhenOneConditionDoesNotMatch() {
        TestRule rule = new TestRule(1L, """
                {
                  "logicalOperator": "AND",
                  "rejectionReasonCode": "AMOUNT_BLOCKED",
                  "rejectionReasonDescription": "Amount is too high",
                  "conditions": [
                    { "field": "taxSource", "operator": "EQUALS", "value": "VAT" },
                    { "field": "amount", "operator": "GREATER_THAN", "value": 1000 }
                  ]
                }
                """, "2026-08-01");

        TestEvaluationResult result = engine.evaluate(List.of(rule), Map.of("taxSource", "VAT", "amount", 500));

        assertThat(result.allowed()).isTrue();
    }

    @Test
    void orRuleBlocksWhenAnyConditionMatches() {
        TestRule rule = new TestRule(1L, """
                {
                  "logicalOperator": "OR",
                  "rejectionReasonCode": "SOURCE_BLOCKED",
                  "rejectionReasonDescription": "Source is blocked",
                  "conditions": [
                    { "field": "taxSource", "operator": "IN", "value": ["VAT", "SALARY_TAX"] },
                    { "field": "amount", "operator": "LESS_THAN", "value": 0 }
                  ]
                }
                """, "2026-08-01");

        TestEvaluationResult result = engine.evaluate(List.of(rule), Map.of("taxSource", "SALARY_TAX", "amount", 100));

        assertThat(result.allowed()).isFalse();
        assertThat(result.rejectionReasonCode()).isEqualTo("SOURCE_BLOCKED");
    }

    @Test
    void latestRestrictionRemovalDateIsReturnedAcrossBlockingRules() {
        TestRule first = new TestRule(1L, blockingRule("FIRST"), "2026-08-01");
        TestRule second = new TestRule(2L, blockingRule("SECOND"), "2026-09-15");

        TestEvaluationResult result = engine.evaluate(List.of(first, second), Map.of("flag", "BLOCK"));

        assertThat(result.allowed()).isFalse();
        assertThat(result.rejectionReasonCode()).isEqualTo("FIRST");
        assertThat(result.restrictionRemovalDate()).isEqualTo("2026-09-15");
    }

    private String blockingRule(String code) {
        return """
                {
                  "logicalOperator": "AND",
                  "rejectionReasonCode": "%s",
                  "rejectionReasonDescription": "Blocked",
                  "conditions": [
                    { "field": "flag", "operator": "EQUALS", "value": "BLOCK" }
                  ]
                }
                """.formatted(code);
    }

    private record TestRule(Long id, String ruleJson, String endDate) {}

    private record TestEvaluationResult(
            boolean allowed,
            String rejectionReasonCode,
            String rejectionReasonDescription,
            String restrictionRemovalDate
    ) {}

    private static class TestRuleEngine extends AbstractRuleEngineService<TestRule, Map<String, Object>> {

        private TestRuleEngine() {
            super(new ObjectMapper());
        }

        private TestEvaluationResult evaluate(List<TestRule> rules, Map<String, Object> context) {
            GenericRuleEvaluationResult result = evaluateRules(
                    rules,
                    context,
                    TestRule::id,
                    TestRule::ruleJson,
                    TestRule::endDate,
                    "test"
            );
            return new TestEvaluationResult(
                    result.allowed(),
                    result.rejectionReasonCode(),
                    result.rejectionReasonDescription(),
                    result.restrictionRemovalDate()
            );
        }

        @Override
        protected Object resolveField(String field, Map<String, Object> context) {
            return context.get(field);
        }
    }
}
