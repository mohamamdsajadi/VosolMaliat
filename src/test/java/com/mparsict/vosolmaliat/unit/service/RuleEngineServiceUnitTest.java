package com.mparsict.vosolmaliat.unit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.request.DebtPaperIssuanceRequest;
import com.mparsict.vosolmaliat.application.dto.request.RejectionRuleRequest;
import com.mparsict.vosolmaliat.application.service.RuleEngineService;
import com.mparsict.vosolmaliat.domain.model.rule.ExecutionPaperRejectionRule;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperRejectionRuleRepository;
import com.mparsict.vosolmaliat.presentation.api.RejectionRuleController;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEngineServiceUnitTest {

    @Test
    void ruleCreatedByAdminRejectsMatchingExecutionPaperIssuance() {
        AtomicReference<ExecutionPaperRejectionRule> storedRule = new AtomicReference<>();
        ExecutionPaperRejectionRuleRepository repository = mutableRepository(storedRule);
        RejectionRuleController controller = new RejectionRuleController(repository);

        var response = controller.create(new RejectionRuleRequest(
                """
                        {
                          "logicalOperator": "AND",
                          "rejectionReasonCode": "TAX_OFFICE_SUSPENDED",
                          "rejectionReasonDescription": "Execution-paper issuance is suspended for this tax office",
                          "conditions": [
                            { "field": "taxAffairsOfficeId", "operator": "EQUALS", "value": 7 }
                          ]
                        }
                        """,
                "2026-01-01",
                "2026-12-31",
                "DOC-2026-10",
                "/rules/doc-2026-10.pdf",
                "Temporary issuance restriction"
        ));

        RuleEngineService.RuleEvaluationResult result =
                new RuleEngineService(repository, new ObjectMapper()).evaluate(sampleRequest(7L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(storedRule.get())
                .extracting(
                        ExecutionPaperRejectionRule::getStartDate,
                        ExecutionPaperRejectionRule::getEndDate,
                        ExecutionPaperRejectionRule::getDocumentNo
                )
                .containsExactly("2026-01-01", "2026-12-31", "DOC-2026-10");
        assertThat(result.canIssue()).isFalse();
        assertThat(result.rejectionReasonCode()).isEqualTo("TAX_OFFICE_SUSPENDED");
        assertThat(result.rejectionReasonDescription())
                .isEqualTo("Execution-paper issuance is suspended for this tax office");
        assertThat(result.restrictionRemovalDate()).isEqualTo("2026-12-31");
    }

    @Test
    void evaluateSupportsTaxAffairsOfficeIdRulesForIssuanceFlow() {
        ExecutionPaperRejectionRule rule = ExecutionPaperRejectionRule.builder()
                .id(1L)
                .endDate("2026-08-01")
                .ruleJson("""
                        {
                          "logicalOperator": "AND",
                          "rejectionReasonCode": "TAX_OFFICE_BLOCKED",
                          "rejectionReasonDescription": "Tax office is blocked",
                          "conditions": [
                            { "field": "taxAffairsOfficeId", "operator": "EQUALS", "value": 7 }
                          ]
                        }
                        """)
                .build();
        RuleEngineService service = new RuleEngineService(repositoryWithRules(List.of(rule)), new ObjectMapper());

        RuleEngineService.RuleEvaluationResult result = service.evaluate(sampleRequest(7L));

        assertThat(result.canIssue()).isFalse();
        assertThat(result.rejectionReasonCode()).isEqualTo("TAX_OFFICE_BLOCKED");
        assertThat(result.restrictionRemovalDate()).isEqualTo("2026-08-01");
    }

    @Test
    void evaluateAllowsIssuanceWhenNoActiveRuleMatches() {
        ExecutionPaperRejectionRule rule = ExecutionPaperRejectionRule.builder()
                .id(1L)
                .ruleJson("""
                        {
                          "conditions": [
                            { "field": "taxAffairsOfficeId", "operator": "EQUALS", "value": 8 }
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

    private ExecutionPaperRejectionRuleRepository mutableRepository(
            AtomicReference<ExecutionPaperRejectionRule> storedRule) {
        return (ExecutionPaperRejectionRuleRepository) Proxy.newProxyInstance(
                ExecutionPaperRejectionRuleRepository.class.getClassLoader(),
                new Class<?>[]{ExecutionPaperRejectionRuleRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "save" -> {
                        ExecutionPaperRejectionRule rule = (ExecutionPaperRejectionRule) args[0];
                        rule.setId(42L);
                        storedRule.set(rule);
                        yield rule;
                    }
                    case "findActiveRules" -> storedRule.get() == null
                            ? List.of()
                            : List.of(storedRule.get());
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private DebtPaperIssuanceRequest sampleRequest(Long taxAffairsOfficeId) {
        return new DebtPaperIssuanceRequest("DEBT-1001",
                "VAT",
                700L,
                taxAffairsOfficeId,
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
                List.of(),
                com.mparsict.vosolmaliat.TestSanimDocuments.document("DEBT-1001", "2026-07-18", "1405", "SPRING", "فعالیت نمونه"));
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
