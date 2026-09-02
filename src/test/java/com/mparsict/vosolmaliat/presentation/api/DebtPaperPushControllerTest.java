package com.mparsict.vosolmaliat.presentation.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.request.DebtPaperPushRequest;
import com.mparsict.vosolmaliat.application.service.DebtPaperPushService;
import com.mparsict.vosolmaliat.common.exception.DebtPaperPushExceptionHandler;
import com.mparsict.vosolmaliat.common.web.DebtPaperTraceFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DebtPaperPushControllerTest {

    private DebtPaperPushService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(DebtPaperPushService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DebtPaperPushController(service))
                .setControllerAdvice(new DebtPaperPushExceptionHandler())
                .addFilters(new DebtPaperTraceFilter())
                .build();
    }

    @Test
    void pushReturnsGatewayEnvelopeWithGeneratedId() throws Exception {
        when(service.push(any(DebtPaperPushRequest.class), anyString())).thenReturn(4654658719685198198L);

        mockMvc.perform(post(DebtPaperTraceFilter.ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-ID"))
                .andExpect(jsonPath("$.apiGatewayData.executionPaperID").value(4654658719685198198L))
                .andExpect(jsonPath("$.apiGatewayTraceID").isNotEmpty())
                .andExpect(jsonPath("$.apiGatewayIsSuccess").value(true))
                .andExpect(jsonPath("$.apiGatewayStatusCode").value(0));

        ArgumentCaptor<DebtPaperPushRequest> request = ArgumentCaptor.forClass(DebtPaperPushRequest.class);
        verify(service).push(request.capture(), anyString());
        assertThat(request.getValue().referenceId()).isEqualTo("1234");
        assertThat(request.getValue().taxAmount()).isEqualTo("1000");
        assertThat(request.getValue().period()).isEqualTo(2);
    }

    @Test
    void pushRejectsInvalidPeriodUsingGatewayEnvelope() throws Exception {
        mockMvc.perform(post(DebtPaperTraceFilter.ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest().replace("\"period\":2", "\"period\":5")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.apiGatewayData.executionPaperID").value(0))
                .andExpect(jsonPath("$.apiGatewayTraceID").isNotEmpty())
                .andExpect(jsonPath("$.apiGatewayIsSuccess").value(true))
                .andExpect(jsonPath("$.apiGatewayStatusCode").value(-7));

        verifyNoInteractions(service);
    }

    private String validRequest() {
        return """
                {
                  "nationalID":"1234567890",
                  "xml":"<DebtPaper><taxpayer><type>NATURAL</type></taxpayer><taxUnitId>7</taxUnitId></DebtPaper>",
                  "hash":"external-hash",
                  "produceDate":"2023-05-17T11:33:45.123Z",
                  "refrenceID":"1234",
                  "followCode":"1234567890",
                  "secondParameter":"284",
                  "year":1400,
                  "period":2,
                  "taxOfficeID":3,
                  "taxAmount":"1000",
                  "taxSource":"TAXB"
                }
                """;
    }
}
