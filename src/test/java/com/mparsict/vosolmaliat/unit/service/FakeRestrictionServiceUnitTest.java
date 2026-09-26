package com.mparsict.vosolmaliat.unit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.service.FakeRestrictionService;
import com.mparsict.vosolmaliat.application.service.security.AuditLogService;
import com.mparsict.vosolmaliat.domain.model.integration.FakeRestrictionRequest;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperRepository;
import com.mparsict.vosolmaliat.domain.repository.FakeRestrictionRequestRepository;
import com.mparsict.vosolmaliat.domain.repository.security.AuditLogRepository;
import com.mparsict.vosolmaliat.infrastructure.camunda.service.CamundaMessageService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FakeRestrictionServiceUnitTest {

    @Test
    void confirmPublishesCamundaMessageUsingRestrictionRequestId() {
        FakeRestrictionRequest request = FakeRestrictionRequest.builder()
                .id(321L)
                .officialGazetteRequestId("official-gazette-700")
                .executionPaperId(99L)
                .status("SENT")
                .relatedPersonCount(2)
                .payload("[]")
                .build();
        CapturingCamundaMessageService messageService = new CapturingCamundaMessageService();
        FakeRestrictionService service = new FakeRestrictionService(
                restrictionRepository(request),
                new ObjectMapper(),
                new AuditLogService(auditLogRepository()),
                emptyExecutionPaperRepository(),
                messageService
        );

        var response = service.confirm(321L);

        assertThat(response.status()).isEqualTo("CONFIRMED");
        assertThat(messageService.restrictionRequestId)
                .isEqualTo("restriction-official-gazette-700");
    }

    private FakeRestrictionRequestRepository restrictionRepository(FakeRestrictionRequest request) {
        return proxy(FakeRestrictionRequestRepository.class, (method, args) -> switch (method) {
            case "findById" -> Optional.of(request);
            case "save" -> args[0];
            default -> defaultValue(returnType(FakeRestrictionRequestRepository.class, method));
        });
    }

    private AuditLogRepository auditLogRepository() {
        return proxy(AuditLogRepository.class, (method, args) ->
                "save".equals(method) ? args[0] : defaultValue(returnType(AuditLogRepository.class, method)));
    }

    private ExecutionPaperRepository emptyExecutionPaperRepository() {
        return proxy(ExecutionPaperRepository.class, (method, args) ->
                "findById".equals(method)
                        ? Optional.empty()
                        : defaultValue(returnType(ExecutionPaperRepository.class, method)));
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

    private static class CapturingCamundaMessageService extends CamundaMessageService {
        private String restrictionRequestId;

        private CapturingCamundaMessageService() {
            super(null);
        }

        @Override
        public void publishRestrictionsApplied(String restrictionRequestId) {
            this.restrictionRequestId = restrictionRequestId;
        }
    }
}
