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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IssuanceControllerTest {

    private static final String EXTERNAL_DEBT_PAPER_PAYLOAD = """
            {
              "EblagLetterNo_tbz": "BL-1403-000125",
              "EblagLetterDate_tbz": "2026-06-01",
              "eblaghstatus_Tbz": "مشاهده شده",
              "EblagLetterrecievdDate_tbz": "2026-06-03",
              "ID_REF_PORTAL": "1234567890",
              "bedehitype": "1",
              "EdareKolTitle_tbz": "اداره کل امور مالیاتی تهران",
              "OmurCode_tbz": "12",
              "VahedCode_tbz": "1203",
              "EdareAddress_tbz": "تهران، خیابان ولیعصر، ساختمان مالیاتی",
              "EdareTel_tbz": "02188776655",
              "KelaseID_tbz": "5",
              "NationalID_tbz": "0012345678",
              "SenderVahedCode_tbz": "1203",
              "FName_tbz": "Ali",
              "LName_tbz": "Ahmadi",
              "CodeMelli_tbz": "0012345678",
              "BirthDate_tbz": "1988-05-12",
              "ZipCode_tbz": "1587541236",
              "tbz_MoadiAddress": "Tehran, Vanak St, No. 45",
              "Economiccode_Tbz": "1234567890",
              "Tbz_Business_reputation": "Ahmadi Trading",
              "CODE_ACTIVITY_tbz": "2",
              "intacode_Tbz": "INTA-001",
              "MoadiTel_tbz": "09121234567",
              "SodurPlace_tbz": "Tehran",
              "SNO_tbz": "A-998877",
              "JarimeDate_tbz": "2026-05-20",
              "GatiNo_tbz": "G-445566",
              "GatiDate_tbz": "2026-05-10",
              "SenderKelaseID_tbz": "5",
              "ElatGatiat_tbz": "عدم پرداخت اقساط مالیاتی",
              "taxtypecode_Tbz": "VAT",
              "tbz_AmalKard": "عملکرد 1403",
              "tbz_Dore": "1403",
              "tbz_AslMaliat": "150000000",
              "tbz_AslAvarez": "30000000",
              "JarimeSabet_tbz": "5000000",
              "JarimeDirkard_tbz": "2000000",
              "penaltycs_Tbz": "1000000",
              "totalpenalty_Tbz": "7000000",
              "penaltyoff_Tbz": "0",
              "Sum_tbz": "187000000",
              "pardakhtoasl_Tbz": "120000000",
              "pardakhtijarime_Tbz": "5000000",
              "tbz_Pardakhti": "125000000",
              "Etebar_tbz": "0",
              "MandeBedehi_tbz": "62000000"
            }
            """;

    private IssuanceRequestService issuanceRequestService;
    private CamundaProcessService camundaProcessService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        issuanceRequestService = mock(IssuanceRequestService.class);
        camundaProcessService = mock(CamundaProcessService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new IssuanceController(issuanceRequestService, camundaProcessService, new ObjectMapper()))
                .build();
    }

    @Test
    void issueMapsExternalDebtPaperPayloadAndStartsProcess() throws Exception {
        ExecutionPaperIssuanceRequest savedRequest = ExecutionPaperIssuanceRequest.builder()
                .id(42L)
                .debtPaperUniqueCode("1234567890")
                .build();
        when(issuanceRequestService.findByHash(anyString())).thenReturn(Optional.empty());
        when(issuanceRequestService.createAndSave(any(DebtPaperIssuanceRequest.class))).thenReturn(savedRequest);
        when(camundaProcessService.startExecutionPaperIssuanceProcess(eq(42L), anyString()))
                .thenReturn(new IssuanceResponse(true, "EP-1403-000001", null, null, null, "برگ اجرا با موفقیت صادر شد"));

        mockMvc.perform(post("/api/v1/debt-papers/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EXTERNAL_DEBT_PAPER_PAYLOAD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.executionPaperUniqueCode").value("EP-1403-000001"));

        ArgumentCaptor<DebtPaperIssuanceRequest> requestCaptor = ArgumentCaptor.forClass(DebtPaperIssuanceRequest.class);
        verify(issuanceRequestService).createAndSave(requestCaptor.capture());
        DebtPaperIssuanceRequest mappedRequest = requestCaptor.getValue();

        assertThat(mappedRequest.debtPaperUniqueCode()).isEqualTo("1234567890");
        assertThat(mappedRequest.debtPaperIssueDate()).isEqualTo("2026-05-10");
        assertThat(mappedRequest.notificationId()).isEqualTo("BL-1403-000125");
        assertThat(mappedRequest.finalizationReason()).isEqualTo("عدم پرداخت اقساط مالیاتی");
        assertThat(mappedRequest.taxSource()).isEqualTo("VAT");
        assertThat(mappedRequest.performanceYear()).isEqualTo("1403");
        assertThat(mappedRequest.performancePeriod()).isEqualTo("1403");
        assertThat(mappedRequest.activityTypeCode()).isEqualTo("2");
        assertThat(mappedRequest.mainTaxBureauId()).isEqualTo(12L);
        assertThat(mappedRequest.taxBureauId()).isEqualTo(1203L);
        assertThat(mappedRequest.taxUnitId()).isEqualTo(5L);
        assertThat(mappedRequest.sendingUnitId()).isEqualTo(1203L);

        assertThat(mappedRequest.taxpayer().type()).isEqualTo("NATURAL");
        assertThat(mappedRequest.taxpayer().nationalCode()).isEqualTo("0012345678");
        assertThat(mappedRequest.taxpayer().firstName()).isEqualTo("Ali");
        assertThat(mappedRequest.taxpayer().lastName()).isEqualTo("Ahmadi");
        assertThat(mappedRequest.taxpayer().birthDate()).isEqualTo("1988-05-12");

        assertThat(mappedRequest.debtInfo().taxAmount()).isEqualTo(150000000.0);
        assertThat(mappedRequest.debtInfo().dutyAmount()).isEqualTo(30000000.0);
        assertThat(mappedRequest.debtInfo().penaltyArticle22()).isEqualTo(5000000.0);
        assertThat(mappedRequest.debtInfo().penaltyArticle23()).isEqualTo(2000000.0);
        assertThat(mappedRequest.debtInfo().nonDeductiblePenalty()).isEqualTo(1000000.0);
        assertThat(mappedRequest.debtInfo().taxPaidAmount()).isEqualTo(120000000.0);
        assertThat(mappedRequest.debtInfo().penaltyPaidAmount()).isEqualTo(5000000.0);
        assertThat(mappedRequest.debtInfo().taxCredit()).isEqualTo(0.0);
        assertThat(mappedRequest.debtInfo().remainingTax()).isEqualTo(62000000.0);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(camundaProcessService).startExecutionPaperIssuanceProcess(eq(42L), payloadCaptor.capture());
        DebtPaperIssuanceRequest processPayload = new ObjectMapper().readValue(payloadCaptor.getValue(), DebtPaperIssuanceRequest.class);
        assertThat(processPayload).isEqualTo(mappedRequest);
    }

    @Test
    void issueRejectsDuplicateExternalDebtPaperPayloadWithoutStartingProcess() throws Exception {
        when(issuanceRequestService.findByHash(anyString()))
                .thenReturn(Optional.of(ExecutionPaperIssuanceRequest.builder().id(7L).build()));

        mockMvc.perform(post("/api/v1/debt-papers/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EXTERNAL_DEBT_PAPER_PAYLOAD))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("DUPLICATE_REQUEST"));

        verifyNoInteractions(camundaProcessService);
    }
}
