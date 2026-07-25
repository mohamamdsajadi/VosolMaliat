package com.mparsict.vosolmaliat.unit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.service.FakeSanimInformService;
import com.mparsict.vosolmaliat.application.service.security.AuditLogService;
import com.mparsict.vosolmaliat.application.security.CurrentUserContext;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaper;
import com.mparsict.vosolmaliat.domain.model.integration.FakeSanimInformRequest;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperIssuanceRequestRepository;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperRepository;
import com.mparsict.vosolmaliat.domain.repository.FakeSanimInformRequestRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FakeSanimInformServiceUnitTest {

    @Test
    void sendExecutionPaperIdPersistsFakeSanimRequestWithExpectedPayload() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        LocalDateTime executionPaperCreatedAt = LocalDateTime.of(2026, 7, 25, 10, 30);
        ExecutionPaper executionPaper = ExecutionPaper.builder()
                .id(99L)
                .executionPaperUniqueCode("EP-99")
                .build();
        executionPaper.setCreatedAt(executionPaperCreatedAt);
        RepositoryStubs repositories = new RepositoryStubs(executionPaper);
        StubAuditLogService auditLogService = new StubAuditLogService();
        FakeSanimInformService service = new FakeSanimInformService(
                repositories.fakeSanimRepository(),
                repositories.executionPaperRepository(),
                repositories.issuanceRequestRepository(),
                auditLogService,
                objectMapper
        );

        var response = service.sendExecutionPaperId(99L);

        FakeSanimInformRequest request = repositories.savedRequest;
        JsonNode payload = objectMapper.readTree(request.getPayload());
        assertThat(request.getExecutionPaperId()).isEqualTo(99L);
        assertThat(request.getMessageType()).isEqualTo("EXECUTION_PAPER_ID");
        assertThat(request.getStatus()).isEqualTo("SENT");
        assertThat(request.getSentAt()).isNotBlank();
        assertThat(payload.get("messageType").asText()).isEqualTo("EXECUTION_PAPER_ID");
        assertThat(payload.get("executionPaperId").asLong()).isEqualTo(99L);
        assertThat(payload.get("executionPaperCode").asText()).isEqualTo("EP-99");
        assertThat(response.executionPaperCreatedAt()).isEqualTo(executionPaperCreatedAt);
        assertThat(auditLogService.action).isEqualTo("EXECUTION_PAPER_ID_SENT_TO_SANIM");
        assertThat(auditLogService.targetType).isEqualTo("EXECUTION_PAPER");
        assertThat(auditLogService.targetId).isEqualTo("99");
        assertThat(auditLogService.newValue).isEqualTo("EP-99");
    }

    private static class RepositoryStubs {
        private final ExecutionPaper executionPaper;
        private FakeSanimInformRequest savedRequest;

        private RepositoryStubs(ExecutionPaper executionPaper) {
            this.executionPaper = executionPaper;
        }

        private FakeSanimInformRequestRepository fakeSanimRepository() {
            return (FakeSanimInformRequestRepository) Proxy.newProxyInstance(
                    FakeSanimInformRequestRepository.class.getClassLoader(),
                    new Class<?>[]{FakeSanimInformRequestRepository.class},
                    (proxy, method, args) -> {
                        if ("save".equals(method.getName())) {
                            savedRequest = (FakeSanimInformRequest) args[0];
                            return savedRequest;
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
        }

        private ExecutionPaperRepository executionPaperRepository() {
            return (ExecutionPaperRepository) Proxy.newProxyInstance(
                    ExecutionPaperRepository.class.getClassLoader(),
                    new Class<?>[]{ExecutionPaperRepository.class},
                    (proxy, method, args) -> "findById".equals(method.getName())
                            ? Optional.of(executionPaper)
                            : defaultValue(method.getReturnType())
            );
        }

        private ExecutionPaperIssuanceRequestRepository issuanceRequestRepository() {
            return (ExecutionPaperIssuanceRequestRepository) Proxy.newProxyInstance(
                    ExecutionPaperIssuanceRequestRepository.class.getClassLoader(),
                    new Class<?>[]{ExecutionPaperIssuanceRequestRepository.class},
                    (proxy, method, args) -> defaultValue(method.getReturnType())
            );
        }
    }

    private static class StubAuditLogService extends AuditLogService {
        private String action;
        private String targetType;
        private String targetId;
        private String newValue;

        private StubAuditLogService() {
            super(null);
        }

        @Override
        public void record(
                CurrentUserContext actor,
                String action,
                String targetType,
                String targetId,
                String oldValue,
                String newValue) {
            this.action = action;
            this.targetType = targetType;
            this.targetId = targetId;
            this.newValue = newValue;
        }
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
