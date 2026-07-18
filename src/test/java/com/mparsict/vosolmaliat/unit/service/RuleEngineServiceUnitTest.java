package com.mparsict.vosolmaliat.unit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.request.DebtPaperIssuanceRequest;
import com.mparsict.vosolmaliat.application.service.RuleEngineService;
import com.mparsict.vosolmaliat.domain.model.rule.ExecutionPaperRejectionRule;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperRejectionRuleRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEngineServiceUnitTest {

    @Test
    void evaluateSupportsTaxUnitIdRulesForIssuanceFlow() {
        ExecutionPaperRejectionRule rule = ExecutionPaperRejectionRule.builder()
                .id(1L)
                .endDate("2026-08-01")
                .ruleJson("""
                        {
                          "logicalOperator": "AND",
                          "rejectionReasonCode": "TAX_UNIT_BLOCKED",
                          "rejectionReasonDescription": "Tax unit is blocked",
                          "conditions": [
                            { "field": "taxUnitId", "operator": "EQUALS", "value": 7 }
                          ]
                        }
                        """)
                .build();
        RuleEngineService service = new RuleEngineService(repositoryWithRules(List.of(rule)), new ObjectMapper());

        RuleEngineService.RuleEvaluationResult result = service.evaluate(sampleRequest(7L));

        assertThat(result.canIssue()).isFalse();
        assertThat(result.rejectionReasonCode()).isEqualTo("TAX_UNIT_BLOCKED");
        assertThat(result.restrictionRemovalDate()).isEqualTo("2026-08-01");
    }

    @Test
    void evaluateAllowsIssuanceWhenNoActiveRuleMatches() {
        ExecutionPaperRejectionRule rule = ExecutionPaperRejectionRule.builder()
                .id(1L)
                .ruleJson("""
                        {
                          "conditions": [
                            { "field": "taxUnitId", "operator": "EQUALS", "value": 8 }
                          ]
                        }
                        """)
                .build();
        RuleEngineService service = new RuleEngineService(repositoryWithRules(List.of(rule)), new ObjectMapper());

        RuleEngineService.RuleEvaluationResult result = service.evaluate(sampleRequest(7L));

        assertThat(result.canIssue()).isTrue();
        assertThat(result.rejectionReasonCode()).isNull();
    }

    private ExecutionPaperRejectionRuleRepository repositoryWithRules(List<ExecutionPaperRejectionRule> rules) {
        return (ExecutionPaperRejectionRuleRepository) Proxy.newProxyInstance(
                ExecutionPaperRejectionRuleRepository.class.getClassLoader(),
                new Class<?>[]{ExecutionPaperRejectionRuleRepository.class},
                (proxy, method, args) -> "findActiveRules".equals(method.getName()) ? rules : defaultValue(method.getReturnType())
        );
    }

    private DebtPaperIssuanceRequest sampleRequest(Long taxUnitId) {
        return new DebtPaperIssuanceRequest(
                "DEBT-1001",
                "2026-07-18",
                null,
                "قطعی",
                "VAT",
                "1405",
                "SPRING",
                "A01",
                "فعالیت نمونه",
                700L,
                70L,
                taxUnitId,
                null,
                new DebtPaperIssuanceRequest.TaxpayerInfo(
                        "NATURAL",
                        "0012345678",
                        null,
                        "علی",
                        "تهرانی",
                        null,
                        null,
                        "1234567890",
                        "تهران",
                        null,
                        null,
                        null,
                        null
                ),
                new DebtPaperIssuanceRequest.DebtInfo(
                        1000d, 200d, 50d, 25d, 0d, 10d, 100d, 0d, 0d, 1185d, 0d, 0d, 0d
                ),
                List.of()
        );
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        return null;
    }
}
