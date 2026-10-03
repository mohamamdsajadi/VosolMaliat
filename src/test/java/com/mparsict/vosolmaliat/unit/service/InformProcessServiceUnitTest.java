package com.mparsict.vosolmaliat.unit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.response.FakeElectronicNotificationResponse;
import com.mparsict.vosolmaliat.application.dto.response.FakeSanimInformResponse;
import com.mparsict.vosolmaliat.application.service.ExecutionPaperService;
import com.mparsict.vosolmaliat.application.service.FakeElectronicNotificationService;
import com.mparsict.vosolmaliat.application.service.FakeSanimInformService;
import com.mparsict.vosolmaliat.application.service.InformProcessService;
import com.mparsict.vosolmaliat.application.service.InformRuleEngineService;
import com.mparsict.vosolmaliat.application.service.security.AuditLogService;
import com.mparsict.vosolmaliat.domain.enums.ExecutionPaperState;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaper;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaperNotification;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaperStateHistory;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperNotificationRepository;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperRepository;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperStateHistoryRepository;
import com.mparsict.vosolmaliat.infrastructure.camunda.ProcessConstants;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InformProcessServiceUnitTest {

    @Test
    void checkInformPossibilityReturnsBpmnVariableConstantsForRejectedResult() {
        StubInformRuleEngineService ruleEngineService = new StubInformRuleEngineService(
                new InformRuleEngineService.InformRuleEvaluationResult(false, "NO_ADDRESS", "Missing address", "2026-09-01"));

        Map<String, Object> variables = service(ruleEngineService).checkInformPossibility(99L);

        assertThat(variables)
                .containsEntry(ProcessConstants.VAR_CAN_INFORM, false)
                .containsEntry(ProcessConstants.VAR_REJECTION_CODE, "NO_ADDRESS")
                .containsEntry(ProcessConstants.VAR_REJECTION_DESC, "Missing address")
                .containsEntry(ProcessConstants.VAR_RESTRICTION_REMOVAL_DATE, "2026-09-01");
    }

    @Test
    void updateInformStatusPersistsInformedStateWithNormalizedTehranDateTime() {
        RepositoryState repositories = new RepositoryState();
        InformProcessService service = service(new StubInformRuleEngineService(null), repositories);

        repositories.executionPaper.getWorkflowState().setNotificationTrackingId("EB-99");
        service.updateInformStatus(99L, true, "2026-07-18T10:30:45Z", null, null);

        assertThat(repositories.executionPaper.getWorkflowState().getInformStatus()).isEqualTo(ExecutionPaperState.INFORMED.name());
        assertThat(repositories.savedNotification.getElectronicNotificationDate()).isEqualTo("2026-07-18T14:00");
        assertThat(repositories.savedExecutionPaper.getCurrentState()).isEqualTo(ExecutionPaperState.INFORMED.name());
        assertThat(repositories.savedHistory.getState()).isEqualTo(ExecutionPaperState.INFORMED.name());
    }

    @Test
    void updateInformStatusPersistsRejectionReasonAndState() {
        RepositoryState repositories = new RepositoryState();
        StubAuditLogService auditLogService = new StubAuditLogService();
        InformProcessService service = service(new StubInformRuleEngineService(null), repositories, auditLogService,
                new StubFakeElectronicNotificationService(), new StubFakeSanimInformService());

        service.updateInformStatus(99L, false, null, "NO_ADDRESS", "Missing address");

        assertThat(repositories.executionPaper.getWorkflowState().getInformStatus()).isEqualTo(ExecutionPaperState.INFORM_REJECTED.name());
        assertThat(repositories.executionPaper.getWorkflowState().getInformRejectionReason()).isEqualTo("NO_ADDRESS: Missing address");
        assertThat(repositories.savedExecutionPaper.getCurrentState()).isEqualTo(ExecutionPaperState.INFORM_REJECTED.name());
        assertThat(auditLogService.action).isEqualTo("EXECUTION_PAPER_INFORM_REJECTED");
        assertThat(auditLogService.newValue).isEqualTo("NO_ADDRESS: Missing address");
    }

    @Test
    void markSentToElectronicNotificationUpdatesLocalStateAndCallsFakeService() {
        RepositoryState repositories = new RepositoryState();
        StubFakeElectronicNotificationService fakeElectronicNotificationService = new StubFakeElectronicNotificationService();
        InformProcessService service = service(new StubInformRuleEngineService(null), repositories,
                new StubAuditLogService(), fakeElectronicNotificationService, new StubFakeSanimInformService());

        service.markSentToElectronicNotification(99L);

        assertThat(repositories.executionPaper.getWorkflowState().getInformStatus()).isEqualTo(ExecutionPaperState.INFORM_SENT.name());
        assertThat(repositories.savedExecutionPaper.getCurrentState()).isEqualTo(ExecutionPaperState.INFORM_SENT.name());
        assertThat(fakeElectronicNotificationService.sentExecutionPaperId).isEqualTo(99L);
    }

    @Test
    void setReInformDateStoresNextDayMidnightDateTimeForBpmnTimer() {
        RepositoryState repositories = new RepositoryState();
        InformProcessService service = service(new StubInformRuleEngineService(null), repositories);

        String reInformDate = service.setReInformDate(99L, "2026-09-01");

        assertThat(reInformDate).isEqualTo("2026-09-02T00:00+03:30");
        assertThat(repositories.executionPaper.getWorkflowState().getReInformDate()).isEqualTo("2026-09-02T00:00+03:30");
    }

    @Test
    void sendInformDateToSanimDelegatesToFakeSanimService() {
        StubFakeSanimInformService fakeSanimInformService = new StubFakeSanimInformService();
        InformProcessService service = service(new StubInformRuleEngineService(null), new RepositoryState(),
                new StubAuditLogService(), new StubFakeElectronicNotificationService(), fakeSanimInformService);

        service.sendInformDateToSanim(99L, "2026-07-18T10:30");

        assertThat(fakeSanimInformService.executionPaperId).isEqualTo(99L);
        assertThat(fakeSanimInformService.informDate).isEqualTo("2026-07-18T10:30");
    }

    private InformProcessService service(StubInformRuleEngineService ruleEngineService) {
        return service(ruleEngineService, new RepositoryState());
    }

    private InformProcessService service(StubInformRuleEngineService ruleEngineService, RepositoryState repositories) {
        return service(ruleEngineService, repositories, new StubAuditLogService(),
                new StubFakeElectronicNotificationService(), new StubFakeSanimInformService());
    }

    private InformProcessService service(
            StubInformRuleEngineService ruleEngineService,
            RepositoryState repositories,
            StubAuditLogService auditLogService,
            StubFakeElectronicNotificationService fakeElectronicNotificationService,
            StubFakeSanimInformService fakeSanimInformService) {
        return new InformProcessService(
                ruleEngineService,
                fakeElectronicNotificationService,
                fakeSanimInformService,
                executionPaperRepository(repositories),
                notificationRepository(repositories),
                stateHistoryRepository(repositories),
                auditLogService
        );
    }

    private ExecutionPaperRepository executionPaperRepository(RepositoryState state) {
        return (ExecutionPaperRepository) Proxy.newProxyInstance(
                ExecutionPaperRepository.class.getClassLoader(),
                new Class<?>[]{ExecutionPaperRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findById" -> Optional.of(state.executionPaper);
                    case "save" -> {
                        state.savedExecutionPaper = (ExecutionPaper) args[0];
                        yield args[0];
                    }
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private ExecutionPaperNotificationRepository notificationRepository(RepositoryState state) {
        return (ExecutionPaperNotificationRepository) Proxy.newProxyInstance(
                ExecutionPaperNotificationRepository.class.getClassLoader(),
                new Class<?>[]{ExecutionPaperNotificationRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByExecutionPaperId" -> Optional.ofNullable(state.notification);
                    case "save" -> {
                        state.savedNotification = (ExecutionPaperNotification) args[0];
                        yield args[0];
                    }
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private ExecutionPaperStateHistoryRepository stateHistoryRepository(RepositoryState state) {
        return (ExecutionPaperStateHistoryRepository) Proxy.newProxyInstance(
                ExecutionPaperStateHistoryRepository.class.getClassLoader(),
                new Class<?>[]{ExecutionPaperStateHistoryRepository.class},
                (proxy, method, args) -> {
                    if ("save".equals(method.getName())) {
                        state.savedHistory = (ExecutionPaperStateHistory) args[0];
                        return args[0];
                    }
                    return defaultValue(method.getReturnType());
                }
        );
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

    private static class RepositoryState {
        private final ExecutionPaper executionPaper = ExecutionPaper.builder()
                .id(99L)
                .executionPaperUniqueCode("EP-99")
                .currentState(ExecutionPaperState.UNASSIGNED.name())
                .build();
        private ExecutionPaperNotification notification;
        private ExecutionPaper savedExecutionPaper;
        private ExecutionPaperNotification savedNotification;
        private ExecutionPaperStateHistory savedHistory;
    }

    private static class StubInformRuleEngineService extends InformRuleEngineService {
        private final InformRuleEvaluationResult result;

        private StubInformRuleEngineService(InformRuleEvaluationResult result) {
            super(null, null, null, null, new ObjectMapper());
            this.result = result;
        }

        @Override
        public InformRuleEvaluationResult evaluate(Long executionPaperId) {
            return result;
        }
    }

    private static class StubFakeElectronicNotificationService extends FakeElectronicNotificationService {
        private Long sentExecutionPaperId;

        private StubFakeElectronicNotificationService() {
            super(null, null, null, null, null, new ObjectMapper());
        }

        @Override
        public FakeElectronicNotificationResponse send(Long executionPaperId) {
            this.sentExecutionPaperId = executionPaperId;
            return new FakeElectronicNotificationResponse(1L, executionPaperId, "EP-99", "EB-99", "SENT", null, null, null, null, null);
        }
    }

    private static class StubFakeSanimInformService extends FakeSanimInformService {
        private Long executionPaperId;
        private String informDate;

        private StubFakeSanimInformService() {
            super(null, null, null, null, new ObjectMapper());
        }

        @Override
        public FakeSanimInformResponse sendInformDate(Long executionPaperId, String informDate) {
            this.executionPaperId = executionPaperId;
            this.informDate = informDate;
            return null;
        }
    }

    private static class StubAuditLogService extends AuditLogService {
        private String action;
        private String newValue;

        private StubAuditLogService() {
            super(null);
        }

        @Override
        public void record(com.mparsict.vosolmaliat.application.security.CurrentUserContext actor,
                           String action,
                           String targetType,
                           String targetId,
                           String oldValue,
                           String newValue) {
            this.action = action;
            this.newValue = newValue;
        }
    }
}
