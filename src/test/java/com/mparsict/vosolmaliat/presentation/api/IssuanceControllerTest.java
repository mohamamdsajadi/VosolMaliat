package com.mparsict.vosolmaliat.presentation.api;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.request.DebtPaperIssuanceRequest;
import com.mparsict.vosolmaliat.application.dto.response.IssuanceResponse;
import com.mparsict.vosolmaliat.application.service.IssuanceRequestService;
import com.mparsict.vosolmaliat.domain.model.rule.ExecutionPaperIssuanceRequest;
import com.mparsict.vosolmaliat.infrastructure.camunda.service.CamundaProcessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class IssuanceControllerTest {
    private final IssuanceRequestService issuance = mock(IssuanceRequestService.class);
    private final CamundaProcessService workflow = mock(CamundaProcessService.class);
    private MockMvc mvc;
    private final String payload = """
        {"debtPaperUniqueCode":"ref-1","taxSource":"VAT","taxBureauId":7,
         "taxpayer":{"type":"NATURAL","nationalCode":"0012345678"},
         "sourceDocument":{"documentNumber":"doc-1","issueDateRaw":"1400/01/01",
            "amounts":{"outstandingDebtAmount":"-12345678901234567890.54","totalTaxAmount":"100",
               "totalDutyAmount":"0","totalPenaltyAmount":"2","totalPaidAmount":"3",
               "totalCreditAmount":"0","totalRemissionAmount":"0"}}}
        """;
    @BeforeEach void setup() { mvc = MockMvcBuilders.standaloneSetup(new IssuanceController(issuance, workflow, new ObjectMapper())).setControllerAdvice(new com.mparsict.vosolmaliat.common.exception.GlobalExceptionHandler()).build(); }
    @Test void usesOnlySourceDocumentAggregatesWithoutRounding() throws Exception {
        when(issuance.findByHash(anyString())).thenReturn(Optional.empty());
        when(issuance.createAndSave(any())).thenReturn(ExecutionPaperIssuanceRequest.builder().id(42L).build());
        when(workflow.startExecutionPaperIssuanceProcess(eq(42L), any())).thenReturn(new IssuanceResponse(true,"EP-1",null,null,null,"ok"));
        mvc.perform(post("/api/v1/debt-papers/issue").contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isOk());
        var captor = ArgumentCaptor.forClass(DebtPaperIssuanceRequest.class);
        verify(issuance).createAndSave(captor.capture());
        assertThat(captor.getValue().sourceDocument().getAmounts().getOutstandingDebtAmount()).isEqualByComparingTo("-12345678901234567890.54");
        assertThat(captor.getValue().taxBureauId()).isEqualTo(7L);
    }
    @Test void incompleteNewAmountsReturn422WithoutPersistenceOrWorkflow() throws Exception {
        mvc.perform(post("/api/v1/debt-papers/issue").contentType(MediaType.APPLICATION_JSON)
                .content(payload.replace("\"totalRemissionAmount\":\"0\"", "\"totalRemissionAmount\":null")))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.error").value("SOURCE_DOCUMENT_INVALID"));
        verifyNoInteractions(workflow, issuance);
    }
    @Test void duplicateDoesNotStartProcess() throws Exception {
        when(issuance.findByHash(anyString())).thenReturn(Optional.of(ExecutionPaperIssuanceRequest.builder().id(1L).build()));
        mvc.perform(post("/api/v1/debt-papers/issue").contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isConflict());
        verifyNoInteractions(workflow);
    }
    @Test void legacyFinancialContractIsRejectedExplicitly() throws Exception {
        mvc.perform(post("/api/v1/debt-papers/issue").contentType(MediaType.APPLICATION_JSON).content("{\"debtInfo\":{\"penaltyArticle22\":1}}"))
                .andExpect(status().isUnprocessableEntity());
        verifyNoInteractions(workflow, issuance);
    }
}
