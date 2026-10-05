package com.mparsict.vosolmaliat.unit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.response.SanimDebtStatusResponse;
import com.mparsict.vosolmaliat.application.dto.response.SanimTaxpayerOptionResponse;
import com.mparsict.vosolmaliat.application.service.DebtStatusUpdateService;
import com.mparsict.vosolmaliat.application.service.SanimAuditService;
import com.mparsict.vosolmaliat.application.service.security.AuditLogService;
import com.mparsict.vosolmaliat.domain.enums.TaxpayerType;
import com.mparsict.vosolmaliat.domain.model.Taxpayer;
import com.mparsict.vosolmaliat.domain.model.execution.DebtInformation;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaper;
import com.mparsict.vosolmaliat.domain.model.execution.FinalTaxDebtPaper;
import com.mparsict.vosolmaliat.domain.model.inquiry.DebtInquiryResult;
import com.mparsict.vosolmaliat.domain.repository.DebtInformationRepository;
import com.mparsict.vosolmaliat.domain.repository.DebtInquiryResultRepository;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperRepository;
import com.mparsict.vosolmaliat.domain.repository.security.AuditLogRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class DebtStatusUpdateServiceUnitTest {

    @Test
    void storesImmutableResponseAndUpdatesDedicatedSanimSnapshot() {
        ExecutionPaper paper = ExecutionPaper.builder()
                .id(99L)
                .finalTaxDebtPaper(FinalTaxDebtPaper.builder()
                        .taxpayer(Taxpayer.builder().id(10L).build()).build())
                .build();
        DebtInformation existingDebt = DebtInformation.builder()
                .executionPaper(paper)
                .currentAmounts(com.mparsict.vosolmaliat.domain.model.sanim.SanimAmounts.builder()
                    .totalTaxAmount(new java.math.BigDecimal("900000"))
                    .totalRemissionAmount(java.math.BigDecimal.ZERO).totalDutyAmount(new java.math.BigDecimal("100000")).build())
                .build();
        AtomicReference<DebtInquiryResult> savedResult = new AtomicReference<>();
        AtomicReference<DebtInformation> savedDebt = new AtomicReference<>();
        ExecutionPaperRepository executionPapers = proxy(ExecutionPaperRepository.class,
                (method, args) -> "findById".equals(method) ? Optional.of(paper) : defaultValue(
                        returnType(ExecutionPaperRepository.class, method)));
        DebtInformationRepository debtInformation = proxy(DebtInformationRepository.class,
                (method, args) -> switch (method) {
                    case "findByExecutionPaperId" -> Optional.of(existingDebt);
                    case "save" -> { savedDebt.set((DebtInformation) args[0]); yield args[0]; }
                    default -> defaultValue(returnType(DebtInformationRepository.class, method));
                });
        DebtInquiryResultRepository inquiryResults = proxy(DebtInquiryResultRepository.class,
                (method, args) -> switch (method) {
                    case "findByRequestId" -> Optional.empty();
                    case "save" -> { savedResult.set((DebtInquiryResult) args[0]); yield args[0]; }
                    default -> defaultValue(returnType(DebtInquiryResultRepository.class, method));
                });
        SanimTaxpayerOptionResponse taxpayer = new SanimTaxpayerOptionResponse(
                10L,
                TaxpayerType.NATURAL,
                "0012345678",
                "علی تهرانی"
        );
        SanimAuditService sanim = new StubSanimAuditService(taxpayer);
        AuditLogRepository auditLogs = proxy(AuditLogRepository.class,
                (method, args) -> "save".equals(method) ? args[0] : defaultValue(
                        returnType(AuditLogRepository.class, method)));
        AuditLogService auditLog = new AuditLogService(auditLogs);
        SanimDebtStatusResponse status = new SanimDebtStatusResponse(
                TaxpayerType.NATURAL,
                "0012345678",
                "علی تهرانی",
                1_000_000L,
                200_000L,
                300_000L,
                100_000L,
                800_000L,
                4,
                LocalDateTime.of(2026, 8, 3, 9, 30)
        );
        DebtStatusUpdateService service = new DebtStatusUpdateService(
                executionPapers,
                debtInformation,
                inquiryResults,
                sanim,
                auditLog,
                new ObjectMapper().findAndRegisterModules()
        , org.mockito.Mockito.mock(com.mparsict.vosolmaliat.domain.repository.DebtInformationHistoryRepository.class));

        service.update(99L, "debt-inquiry-700", status);

        DebtInquiryResult result = savedResult.get();
        assertThat(result).isNotNull();
        assertThat(result.getRequestId()).isEqualTo("debt-inquiry-700");
        assertThat(result.getExecutionPaper().getId()).isEqualTo(99L);
        assertThat(result.getResponseJson()).contains("\"outstandingDebtRials\":800000");
        assertThat(result.getHash()).hasSize(64);

        assertThat(existingDebt.getCurrentAmounts().getTotalTaxAmount()).isEqualByComparingTo("900000");
        assertThat(existingDebt.getCurrentAmounts().getTotalDutyAmount()).isEqualByComparingTo("100000");
        assertThat(existingDebt.getCurrentAmounts().getTotalPenaltyAmount()).isEqualByComparingTo("200000");
        assertThat(existingDebt.getCurrentAmounts().getTotalPaidAmount()).isEqualByComparingTo("300000");
        assertThat(existingDebt.getCurrentAmounts().getTotalCreditAmount()).isEqualByComparingTo("100000");
        assertThat(existingDebt.getCurrentAmounts().getOutstandingDebtAmount()).isEqualByComparingTo("800000");
        assertThat(existingDebt.getCurrentAmountsAsOf()).isEqualTo(status.lastActivityAt());
        assertThat(existingDebt.getLastDebtInquiryAt()).isNotNull();
        assertThat(savedDebt.get()).isSameAs(existingDebt);
    }

    private static class StubSanimAuditService extends SanimAuditService {
        private final SanimTaxpayerOptionResponse taxpayer;

        private StubSanimAuditService(SanimTaxpayerOptionResponse taxpayer) {
            super(null, null, null);
            this.taxpayer = taxpayer;
        }

        @Override
        public SanimTaxpayerOptionResponse taxpayer(Long taxpayerId) {
            return taxpayer;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> type, Handler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> handler.invoke(method.getName(), args));
    }

    private Class<?> returnType(Class<?> type, String methodName) {
        return java.util.Arrays.stream(type.getMethods())
                .filter(method -> method.getName().equals(methodName))
                .findFirst()
                .map(java.lang.reflect.Method::getReturnType)
                .orElse(Object.class);
    }

    private Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == long.class) return 0L;
        if (type == int.class) return 0;
        return null;
    }

    private interface Handler {
        Object invoke(String method, Object[] args);
    }
}
