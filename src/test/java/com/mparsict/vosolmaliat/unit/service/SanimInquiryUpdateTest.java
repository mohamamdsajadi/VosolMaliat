package com.mparsict.vosolmaliat.unit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.response.*;
import com.mparsict.vosolmaliat.application.service.*;
import com.mparsict.vosolmaliat.application.service.security.AuditLogService;
import com.mparsict.vosolmaliat.domain.enums.TaxpayerType;
import com.mparsict.vosolmaliat.domain.model.Taxpayer;
import com.mparsict.vosolmaliat.domain.model.execution.*;
import com.mparsict.vosolmaliat.domain.model.inquiry.DebtInquiryResult;
import com.mparsict.vosolmaliat.domain.model.sanim.SanimAmounts;
import com.mparsict.vosolmaliat.domain.repository.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SanimInquiryUpdateTest {
    @Test void partialResponsesPreserveInitialAndUnspecifiedValuesAndOldOrRepeatedResultsNeverRevertCurrent() {
        var papers = mock(ExecutionPaperRepository.class);
        var debts = mock(DebtInformationRepository.class);
        var results = mock(DebtInquiryResultRepository.class);
        var sanim = mock(SanimAuditService.class);
        var audit = mock(AuditLogService.class);
        var initial = SanimAmounts.builder().outstandingDebtAmount(new BigDecimal("100.123"))
                .totalTaxAmount(new BigDecimal("90.123")).totalRemissionAmount(new BigDecimal("1.23")).build();
        var paper = ExecutionPaper.builder().id(1L).taxpayer(Taxpayer.builder().id(2L).build()).build();
        var debt = DebtInformation.builder().executionPaper(paper).currentAmounts(initial.copy()).build();
        when(papers.findById(1L)).thenReturn(Optional.of(paper));
        when(debts.findByExecutionPaperId(1L)).thenReturn(Optional.of(debt));
        when(sanim.taxpayer(2L)).thenReturn(new SanimTaxpayerOptionResponse(2L, TaxpayerType.NATURAL, "1234567890", "name"));
        Map<String, DebtInquiryResult> history = new HashMap<>();
        when(results.findByRequestId(anyString())).thenAnswer(call -> Optional.ofNullable(history.get(call.getArgument(0))));
        when(results.save(any())).thenAnswer(call -> { DebtInquiryResult result = call.getArgument(0); history.put(result.getRequestId(), result); return result; });
        var service = new DebtStatusUpdateService(papers, debts, results, sanim, audit, new ObjectMapper().findAndRegisterModules(), mock(com.mparsict.vosolmaliat.domain.repository.DebtInformationHistoryRepository.class));
        var first = response("-9.876543210987654321", LocalDateTime.of(2026, 9, 20, 10, 0));
        service.update(1L, "first", first);
        assertThat(debt.getCurrentAmounts().getOutstandingDebtAmount()).isEqualByComparingTo("-9.876543210987654321");
        assertThat(debt.getCurrentAmounts().getTotalTaxAmount()).isEqualByComparingTo("90.123");
        assertThat(debt.getCurrentAmounts().getTotalRemissionAmount()).isEqualByComparingTo("1.23");
        assertThat(initial.getOutstandingDebtAmount()).isEqualByComparingTo("100.123");
        service.update(1L, "newer", response("20", first.lastActivityAt().plusDays(1)));
        service.update(1L, "first", first);
        service.update(1L, "older", response("99", first.lastActivityAt().minusDays(1)));
        service.update(1L, "undated", response("999", null));
        assertThat(debt.getCurrentAmounts().getOutstandingDebtAmount()).isEqualByComparingTo("20");
        assertThat(history).hasSize(4);
        assertThat(history.get("first").getAmounts().getOutstandingDebtAmount()).isEqualByComparingTo("-9.876543210987654321");
        assertThat(history.get("first").getResponseJson()).contains("-9.876543210987654321");
        verify(debts, times(2)).save(debt);
        assertThatThrownBy(() -> service.update(1L, "first", response("123", first.lastActivityAt())))
                .hasMessageContaining("پاسخ دیگری");
    }
    private SanimDebtStatusResponse response(String outstanding, LocalDateTime date) {
        return new SanimDebtStatusResponse(TaxpayerType.NATURAL, "1234567890", "name", 0, 0, 0, 0, 0, 1, date,
                SanimAmounts.builder().outstandingDebtAmount(new BigDecimal(outstanding)).build());
    }
}
