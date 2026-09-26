package com.mparsict.vosolmaliat.unit.service;

import com.mparsict.vosolmaliat.application.dto.response.SanimDebtStatusResponse;
import com.mparsict.vosolmaliat.application.service.SanimAuditService;
import com.mparsict.vosolmaliat.application.service.security.AuditLogService;
import com.mparsict.vosolmaliat.common.exception.BusinessException;
import com.mparsict.vosolmaliat.domain.enums.TaxpayerType;
import com.mparsict.vosolmaliat.domain.model.Taxpayer;
import com.mparsict.vosolmaliat.domain.model.integration.SanimAuditEntry;
import com.mparsict.vosolmaliat.domain.model.person.NaturalPerson;
import com.mparsict.vosolmaliat.domain.repository.SanimAuditEntryRepository;
import com.mparsict.vosolmaliat.domain.repository.TaxpayerRepository;
import com.mparsict.vosolmaliat.domain.repository.security.AuditLogRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class SanimAuditServiceUnitTest {

    @Test
    void inquiryReturnsAllRialComponentsAndNeverReturnsNegativeOutstandingDebt() {
        List<SanimAuditEntry> entries = List.of(
                entry(SanimAuditEntry.EntryType.DEBT, 1_000_000L, 4),
                entry(SanimAuditEntry.EntryType.PENALTY, 200_000L, 3),
                entry(SanimAuditEntry.EntryType.PAYMENT, 900_000L, 2),
                entry(SanimAuditEntry.EntryType.CREDIT, 500_000L, 1)
        );
        SanimAuditService service = service(entries);

        SanimDebtStatusResponse result = service.inquire(10L);

        assertThat(result.taxpayerType()).isEqualTo(TaxpayerType.NATURAL);
        assertThat(result.taxpayerIdentifier()).isEqualTo("0012345678");
        assertThat(result.totalDebtRials()).isEqualTo(1_000_000L);
        assertThat(result.totalPenaltyRials()).isEqualTo(200_000L);
        assertThat(result.totalPaymentRials()).isEqualTo(900_000L);
        assertThat(result.totalCreditRials()).isEqualTo(500_000L);
        assertThat(result.outstandingDebtRials()).isZero();
        assertThat(result.entryCount()).isEqualTo(4);
    }

    @Test
    void inquiryRaisesErrorWhenSanimHasNoHistoryForTaxpayer() {
        SanimAuditService service = service(List.of());

        assertThatThrownBy(() -> service.inquire(10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("سابقه بدهی مؤدی در سنیم یافت نشد");
    }

    private SanimAuditService service(List<SanimAuditEntry> entries) {
        NaturalPerson person = NaturalPerson.builder().id(2L).nationalCode("0012345678")
                .firstName("علی").lastName("تهرانی").fatherName("حسن").birthDate("1370-01-01").build();
        Taxpayer taxpayer = Taxpayer.builder().id(10L).naturalPerson(person).build();
        TaxpayerRepository taxpayers = proxy(TaxpayerRepository.class, (method, args) -> switch (method) {
            case "findById" -> Optional.of(taxpayer);
            case "findAll" -> List.of(taxpayer);
            default -> defaultValue(returnType(TaxpayerRepository.class, method));
        });
        SanimAuditEntryRepository auditEntries = proxy(SanimAuditEntryRepository.class, (method, args) ->
                method.startsWith("findAllByTaxpayerType") ? entries : defaultValue(returnType(SanimAuditEntryRepository.class, method)));
        AuditLogRepository auditLogs = proxy(AuditLogRepository.class, (method, args) -> args == null ? null : args[0]);
        return new SanimAuditService(auditEntries, taxpayers, new AuditLogService(auditLogs));
    }

    private SanimAuditEntry entry(SanimAuditEntry.EntryType type, long amount, int daysAgo) {
        return SanimAuditEntry.builder().id((long) daysAgo).taxpayerType(TaxpayerType.NATURAL)
                .taxpayerIdentifier("0012345678").taxpayerDisplayName("علی تهرانی")
                .entryType(type).amountRials(amount).occurredAt(LocalDateTime.now().minusDays(daysAgo)).build();
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> type, Handler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> handler.invoke(method.getName(), args));
    }

    private Class<?> returnType(Class<?> type, String methodName) {
        return java.util.Arrays.stream(type.getMethods()).filter(m -> m.getName().equals(methodName))
                .findFirst().map(java.lang.reflect.Method::getReturnType).orElse(Object.class);
    }

    private Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == long.class) return 0L;
        if (type == int.class) return 0;
        return null;
    }

    private interface Handler { Object invoke(String method, Object[] args); }
}
