package com.mparsict.vosolmaliat.unit.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.request.DebtPaperIssuanceRequest;
import com.mparsict.vosolmaliat.application.service.RuleEngineService;
import com.mparsict.vosolmaliat.domain.model.rule.ExecutionPaperRejectionRule;
import com.mparsict.vosolmaliat.domain.model.sanim.*;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperRejectionRuleRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SanimRulePrecisionTest {
    @Test void numericRulesCompareExactDecimalsAndUnmappedLegacyRuleBlocksProcessing() {
        var repository = mock(ExecutionPaperRejectionRuleRepository.class);
        var engine = new RuleEngineService(repository, new ObjectMapper());
        var request = mock(DebtPaperIssuanceRequest.class);
        var document = com.mparsict.vosolmaliat.TestSanimDocuments.document("doc", "1400/01/01", "1400", "1", "activity");
        document.getAmounts().setOutstandingDebtAmount(new BigDecimal("9007199254740993.11"));
        when(request.sourceDocument()).thenReturn(document);
        var rule = ExecutionPaperRejectionRule.builder().id(1L).ruleJson("""
                {"conditions":[{"field":"debtAmount","operator":"LESS_THAN","value":9007199254740993.12}],"rejectionReasonCode":"R","rejectionReasonDescription":"below threshold"}
                """).build();
        when(repository.findActiveRules(anyString())).thenReturn(List.of(rule));
        assertThat(engine.evaluate(request).canIssue()).isFalse();
        rule.setRuleJson(rule.getRuleJson().replace("debtAmount", "totalFinalizedAmount"));
        assertThatThrownBy(() -> engine.evaluate(request)).hasMessageContaining("removed rule field");
    }
}
