package com.mparsict.vosolmaliat.unit.camunda;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.request.DebtPaperIssuanceRequest;
import com.mparsict.vosolmaliat.application.service.ExecutionPaperService;
import com.mparsict.vosolmaliat.application.service.FakeOfficialGazetteService;
import com.mparsict.vosolmaliat.application.service.FakeSanimInformService;
import com.mparsict.vosolmaliat.application.service.InformProcessService;
import com.mparsict.vosolmaliat.application.service.IssuanceRequestService;
import com.mparsict.vosolmaliat.application.service.RuleEngineService;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaper;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperRepository;
import com.mparsict.vosolmaliat.domain.repository.TaxUnitRepository;
import com.mparsict.vosolmaliat.infrastructure.camunda.ProcessConstants;
import com.mparsict.vosolmaliat.infrastructure.camunda.worker.CheckInformPossibilityWorker;
import com.mparsict.vosolmaliat.infrastructure.camunda.worker.CheckIssuancePossibilityWorker;
import com.mparsict.vosolmaliat.infrastructure.camunda.worker.IssueExecutionPaperWorker;
import com.mparsict.vosolmaliat.infrastructure.camunda.worker.SendExecutionPaperIdToSanimWorker;
import com.mparsict.vosolmaliat.infrastructure.camunda.worker.SendExecutionPaperToElectronicNotificationWorker;
import com.mparsict.vosolmaliat.infrastructure.camunda.worker.SendInformDataToSanimWorker;
import com.mparsict.vosolmaliat.infrastructure.camunda.worker.SendOfficialGazetteBoardMembersRequestWorker;
import com.mparsict.vosolmaliat.infrastructure.camunda.worker.SendRejectionResponseWorker;
import com.mparsict.vosolmaliat.infrastructure.camunda.worker.SetDebtInquiryDateWorker;
import com.mparsict.vosolmaliat.infrastructure.camunda.worker.SetReInformDateWorker;
import com.mparsict.vosolmaliat.infrastructure.camunda.worker.UpdateInformStatusWorker;
import io.camunda.client.api.response.ActivatedJob;
import io.camunda.client.api.worker.JobClient;
import io.camunda.client.annotation.JobWorker;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CamundaJobWorkersUnitTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void manuallyCompletedWorkersDisableFrameworkAutoCompletion() throws Exception {
        List<Class<?>> workers = List.of(
                CheckIssuancePossibilityWorker.class,
                IssueExecutionPaperWorker.class,
                SendRejectionResponseWorker.class,
                SendExecutionPaperIdToSanimWorker.class,
                CheckInformPossibilityWorker.class,
                SendExecutionPaperToElectronicNotificationWorker.class,
                UpdateInformStatusWorker.class,
                SetReInformDateWorker.class,
                SetDebtInquiryDateWorker.class,
                SendInformDataToSanimWorker.class,
                SendOfficialGazetteBoardMembersRequestWorker.class
        );

        for (Class<?> worker : workers) {
            JobWorker annotation = worker
                    .getMethod("handle", JobClient.class, ActivatedJob.class)
                    .getAnnotation(JobWorker.class);
            assertThat(annotation)
                    .as("%s handle method must be a job worker", worker.getSimpleName())
                    .isNotNull();
            assertThat(annotation.autoComplete())
                    .as("%s manually completes jobs", worker.getSimpleName())
                    .isFalse();
        }
    }

    @Test
    void checkIssuancePossibilityCompletesJobWithRuleEngineResult() throws Exception {
        StubRuleEngineService ruleEngineService = new StubRuleEngineService(
                new RuleEngineService.RuleEvaluationResult(false, "BLOCKED", "Blocked by rule", "2026-08-01"));
        CheckIssuancePossibilityWorker worker = new CheckIssuancePossibilityWorker(ruleEngineService, objectMapper);
        CamundaJobClientStub client = new CamundaJobClientStub();

        worker.handle(client.proxy(), job(Map.of(
                ProcessConstants.VAR_DEBT_PAPER_PAYLOAD,
                objectMapper.convertValue(sampleDebtPaper(), Map.class)
        )));

        assertThat(ruleEngineService.evaluatedRequest.debtPaperUniqueCode()).isEqualTo("DEBT-1001");
        assertThat(client.completed).isTrue();
        assertThat(client.completeVariables)
                .containsEntry(ProcessConstants.VAR_CAN_ISSUE, false)
                .containsEntry(ProcessConstants.VAR_REJECTION_CODE, "BLOCKED")
                .containsEntry(ProcessConstants.VAR_REJECTION_DESC, "Blocked by rule")
                .containsEntry(ProcessConstants.VAR_RESTRICTION_REMOVAL_DATE, "2026-08-01");
    }

    @Test
    void issueExecutionPaperCompletesJobWithExecutionPaperAndOrganizationVariables() throws Exception {
        StubExecutionPaperService executionPaperService = new StubExecutionPaperService(ExecutionPaper.builder()
                .id(99L)
                .executionPaperUniqueCode("EP-99")
                .taxUnit(com.mparsict.vosolmaliat.domain.model.masterdata.TaxUnit.builder().id(7L).build())
                .build());
        IssueExecutionPaperWorker worker = new IssueExecutionPaperWorker(
                executionPaperService,
                new StubIssuanceRequestService(),
                taxUnitRepository(70L, 700L),
                objectMapper
        );
        CamundaJobClientStub client = new CamundaJobClientStub();

        worker.handle(client.proxy(), job(Map.of(
                ProcessConstants.VAR_DEBT_PAPER_PAYLOAD,
                objectMapper.convertValue(sampleLegalDebtPaper(), Map.class),
                ProcessConstants.VAR_ISSUANCE_REQUEST_ID, 123L
        )));

        assertThat(executionPaperService.createdRequest.debtPaperUniqueCode()).isEqualTo("DEBT-1001");
        assertThat(client.completed).isTrue();
        assertThat(client.completeVariables)
                .containsEntry(ProcessConstants.VAR_EXECUTION_PAPER_ID, 99L)
                .containsEntry(ProcessConstants.VAR_EXECUTION_PAPER_CODE, "EP-99")
                .containsEntry(ProcessConstants.VAR_TAX_UNIT_ID, 7L)
                .containsEntry(ProcessConstants.VAR_TAX_AFFAIRS_OFFICE_ID, 70L)
                .containsEntry(ProcessConstants.VAR_GENERAL_DIRECTORATE_ID, 700L)
                .containsEntry(ProcessConstants.VAR_GENERAL_OFFICE_HEAD_CANDIDATE_GROUPS, List.of("GENERAL_OFFICE_HEAD:700"))
                .containsEntry(ProcessConstants.VAR_TAX_OFFICE_HEAD_CANDIDATE_GROUPS, List.of("TAX_OFFICE_HEAD:70"))
                .containsEntry(ProcessConstants.VAR_TAX_UNIT_HEAD_CANDIDATE_GROUPS, List.of("TAX_UNIT_HEAD:7"))
                .containsEntry(ProcessConstants.VAR_TAX_EXPERT_CANDIDATE_GROUPS, List.of("TAX_EXPERT:7"))
                .containsEntry(ProcessConstants.VAR_LEGAL_PERSON_NATIONAL_ID, "14001234567")
                .containsEntry(ProcessConstants.VAR_DEBT_PAPER_DATE, "2026-07-18")
                .containsKey(ProcessConstants.VAR_DEBT_PAPER_PAYLOAD)
                .containsEntry(ProcessConstants.VAR_SUCCESS, true);
    }

    @Test
    void sendRejectionResponseMarksIssuanceRejectedAndNotifiesFakeSanim() {
        StubIssuanceRequestService issuanceRequestService = new StubIssuanceRequestService();
        StubFakeSanimInformService fakeSanimInformService = new StubFakeSanimInformService();
        SendRejectionResponseWorker worker = new SendRejectionResponseWorker(issuanceRequestService, fakeSanimInformService);
        CamundaJobClientStub client = new CamundaJobClientStub();

        worker.handle(client.proxy(), job(Map.of(
                ProcessConstants.VAR_ISSUANCE_REQUEST_ID, 123L,
                ProcessConstants.VAR_REJECTION_CODE, "RULE_A",
                ProcessConstants.VAR_REJECTION_DESC, "Rejected",
                ProcessConstants.VAR_RESTRICTION_REMOVAL_DATE, "2026-09-01"
        )));

        assertThat(issuanceRequestService.rejectedRequestId).isEqualTo(123L);
        assertThat(issuanceRequestService.rejectionReason)
                .isEqualTo("RULE_A: Rejected (restriction removal date: 2026-09-01)");
        assertThat(fakeSanimInformService.issuanceRejectionRequestId).isEqualTo(123L);
        assertThat(client.completed).isTrue();
        assertThat(client.completeVariables).containsEntry(ProcessConstants.VAR_SUCCESS, false);
    }

    @Test
    void sendExecutionPaperIdToSanimDelegatesToFakeSanimAndCompletesJob() {
        StubFakeSanimInformService fakeSanimInformService = new StubFakeSanimInformService();
        SendExecutionPaperIdToSanimWorker worker = new SendExecutionPaperIdToSanimWorker(
                fakeSanimInformService,
                null
        );
        CamundaJobClientStub client = new CamundaJobClientStub();

        worker.handle(client.proxy(), job(Map.of(ProcessConstants.VAR_EXECUTION_PAPER_ID, 99L)));

        assertThat(fakeSanimInformService.executionPaperId).isEqualTo(99L);
        assertThat(client.completed).isTrue();
    }

    @Test
    void sendExecutionPaperIdToSanimRecoversMissingIdFromDebtPaperPayload() {
        StubFakeSanimInformService fakeSanimInformService = new StubFakeSanimInformService();
        SendExecutionPaperIdToSanimWorker worker = new SendExecutionPaperIdToSanimWorker(
                fakeSanimInformService,
                executionPaperRepositoryByDebtPaperCode("DEBT-1001", 99L)
        );
        CamundaJobClientStub client = new CamundaJobClientStub();

        worker.handle(client.proxy(), job(Map.of(
                ProcessConstants.VAR_DEBT_PAPER_PAYLOAD,
                Map.of("debtPaperUniqueCode", "DEBT-1001")
        )));

        assertThat(fakeSanimInformService.executionPaperId).isEqualTo(99L);
        assertThat(client.completed).isTrue();
    }

    @Test
    void officialGazetteWorkerCreatesStableRequestIdWhenProcessVariableIsMissing() {
        StubFakeOfficialGazetteService service = new StubFakeOfficialGazetteService();
        SendOfficialGazetteBoardMembersRequestWorker worker =
                new SendOfficialGazetteBoardMembersRequestWorker(
                        service,
                        executionPaperRepositoryByDebtPaperCode("DEBT-1001", 99L)
                );
        CamundaJobClientStub client = new CamundaJobClientStub();

        worker.handle(client.proxy(), job(Map.of(
                ProcessConstants.VAR_DEBT_PAPER_PAYLOAD, Map.of(
                        "debtPaperUniqueCode", "DEBT-1001",
                        "debtPaperIssueDate", "۱۴۰۵/۰۱/۰۱",
                        "taxpayer", Map.of("nationalId", "14001234567")
                )
        )));

        assertThat(service.requestId).isEqualTo("official-gazette-700");
        assertThat(service.executionPaperId).isEqualTo(99L);
        assertThat(service.legalPersonNationalId).isEqualTo("14001234567");
        assertThat(service.debtPaperDate).isEqualTo("1405-01-01");
        assertThat(client.completed).isTrue();
        assertThat(client.completeVariables)
                .containsEntry(
                        ProcessConstants.VAR_OFFICIAL_GAZETTE_REQUEST_ID,
                        "official-gazette-700"
                )
                .containsEntry(ProcessConstants.VAR_EXECUTION_PAPER_ID, 99L);
        assertThat(client.completeVariables)
                .containsEntry(ProcessConstants.VAR_LEGAL_PERSON_NATIONAL_ID, "14001234567")
                .containsEntry(ProcessConstants.VAR_DEBT_PAPER_DATE, "1405-01-01");
    }

    @Test
    void checkInformPossibilityCompletesJobWithInformRuleVariables() {
        StubInformProcessService service = new StubInformProcessService();
        service.checkInformResult = Map.of(ProcessConstants.VAR_CAN_INFORM, true);
        CheckInformPossibilityWorker worker = new CheckInformPossibilityWorker(service, null);
        CamundaJobClientStub client = new CamundaJobClientStub();

        worker.handle(client.proxy(), job(Map.of(ProcessConstants.VAR_EXECUTION_PAPER_ID, "99")));

        assertThat(service.checkedExecutionPaperId).isEqualTo(99L);
        assertThat(client.completed).isTrue();
        assertThat(client.completeVariables)
                .containsEntry(ProcessConstants.VAR_CAN_INFORM, true)
                .containsEntry(ProcessConstants.VAR_EXECUTION_PAPER_ID, 99L);
    }

    @Test
    void checkInformPossibilityRecoversExecutionPaperIdAndPublishesCanInform() {
        StubInformProcessService service = new StubInformProcessService();
        service.checkInformResult = Map.of(ProcessConstants.VAR_CAN_INFORM, true);
        CheckInformPossibilityWorker worker = new CheckInformPossibilityWorker(
                service,
                executionPaperRepositoryByDebtPaperCode("DEBT-1001", 99L)
        );
        CamundaJobClientStub client = new CamundaJobClientStub();

        worker.handle(client.proxy(), job(Map.of(
                ProcessConstants.VAR_DEBT_PAPER_PAYLOAD,
                Map.of("debtPaperUniqueCode", "DEBT-1001")
        )));

        assertThat(service.checkedExecutionPaperId).isEqualTo(99L);
        assertThat(client.completeVariables)
                .containsEntry(ProcessConstants.VAR_EXECUTION_PAPER_ID, 99L)
                .containsEntry(ProcessConstants.VAR_CAN_INFORM, true);
    }

    @Test
    void checkInformPossibilityFailsBeforeGatewayWhenCanInformIsMissing() {
        StubInformProcessService service = new StubInformProcessService();
        service.checkInformResult = Map.of();
        CheckInformPossibilityWorker worker = new CheckInformPossibilityWorker(service, null);
        CamundaJobClientStub client = new CamundaJobClientStub();

        worker.handle(client.proxy(), job(Map.of(ProcessConstants.VAR_EXECUTION_PAPER_ID, 99L)));

        assertThat(client.failed).isTrue();
        assertThat(client.errorMessage).contains(ProcessConstants.VAR_CAN_INFORM);
    }

    @Test
    void sendExecutionPaperToElectronicNotificationMarksSentAndCompletesJob() {
        StubInformProcessService service = new StubInformProcessService();
        SendExecutionPaperToElectronicNotificationWorker worker =
                new SendExecutionPaperToElectronicNotificationWorker(service, null);
        CamundaJobClientStub client = new CamundaJobClientStub();

        worker.handle(client.proxy(), job(Map.of(ProcessConstants.VAR_EXECUTION_PAPER_ID, 99L)));

        assertThat(service.sentToElectronicNotificationExecutionPaperId).isEqualTo(99L);
        assertThat(client.completed).isTrue();
    }

    @Test
    void updateInformStatusPassesInformVariablesToServiceAndCompletesJob() {
        StubInformProcessService service = new StubInformProcessService();
        UpdateInformStatusWorker worker = new UpdateInformStatusWorker(service, null);
        CamundaJobClientStub client = new CamundaJobClientStub();

        worker.handle(client.proxy(), job(Map.of(
                ProcessConstants.VAR_EXECUTION_PAPER_ID, 99L,
                ProcessConstants.VAR_CAN_INFORM, false,
                ProcessConstants.VAR_INFORM_DATE, "2026-07-18T10:30:00",
                ProcessConstants.VAR_REJECTION_CODE, "NO_ADDRESS",
                ProcessConstants.VAR_REJECTION_DESC, "Missing address"
        )));

        assertThat(service.updatedExecutionPaperId).isEqualTo(99L);
        assertThat(service.updatedCanInform).isFalse();
        assertThat(service.updatedInformDate).isNull();
        assertThat(service.updatedRejectionCode).isEqualTo("NO_ADDRESS");
        assertThat(service.updatedRejectionDesc).isEqualTo("Missing address");
        assertThat(client.completed).isTrue();
    }

    @Test
    void setReInformDateCompletesJobWithReInformDateVariable() {
        StubInformProcessService service = new StubInformProcessService();
        service.reInformDate = "2026-09-02T00:00:00+03:30";
        SetReInformDateWorker worker = new SetReInformDateWorker(service, null);
        CamundaJobClientStub client = new CamundaJobClientStub();

        worker.handle(client.proxy(), job(Map.of(
                ProcessConstants.VAR_EXECUTION_PAPER_ID, 99L,
                ProcessConstants.VAR_RESTRICTION_REMOVAL_DATE, "2026-09-01"
        )));

        assertThat(service.reInformExecutionPaperId).isEqualTo(99L);
        assertThat(service.reInformRestrictionRemovalDate).isEqualTo("2026-09-01");
        assertThat(client.completed).isTrue();
        assertThat(client.completeVariables)
                .containsEntry(ProcessConstants.VAR_RE_INFORM_DATE, "2026-09-02T00:00:00+03:30");
    }

    @Test
    void setDebtInquiryDateCompletesJobWithNextDebtInquiryDateVariable() {
        StubInformProcessService service = new StubInformProcessService();
        service.nextDebtInquiryDate = "2026-08-18";
        SetDebtInquiryDateWorker worker = new SetDebtInquiryDateWorker(service, null);
        CamundaJobClientStub client = new CamundaJobClientStub();

        worker.handle(client.proxy(), job(Map.of(
                ProcessConstants.VAR_EXECUTION_PAPER_ID, 99L,
                ProcessConstants.VAR_INFORM_DATE, "2026-07-18T10:30:00"
        )));

        assertThat(service.nextInquiryExecutionPaperId).isEqualTo(99L);
        assertThat(service.nextInquiryInformDate).isEqualTo("2026-07-18T10:30:00");
        assertThat(client.completed).isTrue();
        assertThat(client.completeVariables)
                .containsEntry(ProcessConstants.VAR_NEXT_DEBT_INQUIRY_DATE, "2026-08-18");
    }

    @Test
    void sendInformDataToSanimPassesInformDateToServiceAndCompletesJob() {
        StubInformProcessService service = new StubInformProcessService();
        SendInformDataToSanimWorker worker = new SendInformDataToSanimWorker(service, null);
        CamundaJobClientStub client = new CamundaJobClientStub();

        worker.handle(client.proxy(), job(Map.of(
                ProcessConstants.VAR_EXECUTION_PAPER_ID, 99L,
                ProcessConstants.VAR_INFORM_DATE, "2026-07-18T10:30:00"
        )));

        assertThat(service.sanimExecutionPaperId).isEqualTo(99L);
        assertThat(service.sanimInformDate).isEqualTo("2026-07-18T10:30:00");
        assertThat(client.completed).isTrue();
    }

    @Test
    void sendInformDataToSanimRecoversExecutionPaperIdFromDebtPaperPayload() {
        StubInformProcessService service = new StubInformProcessService();
        SendInformDataToSanimWorker worker = new SendInformDataToSanimWorker(
                service,
                executionPaperRepositoryByDebtPaperCode("DEBT-1001", 99L)
        );
        CamundaJobClientStub client = new CamundaJobClientStub();

        worker.handle(client.proxy(), job(Map.of(
                ProcessConstants.VAR_DEBT_PAPER_PAYLOAD, Map.of("debtPaperUniqueCode", "DEBT-1001"),
                ProcessConstants.VAR_INFORM_DATE, "2026-07-18T10:30:00"
        )));

        assertThat(service.sanimExecutionPaperId).isEqualTo(99L);
        assertThat(client.completeVariables)
                .containsEntry(ProcessConstants.VAR_EXECUTION_PAPER_ID, 99L);
    }

    @Test
    void workerFailureSendsCamundaFailCommand() {
        CheckInformPossibilityWorker worker = new CheckInformPossibilityWorker(
                new StubInformProcessService(),
                null
        );
        CamundaJobClientStub client = new CamundaJobClientStub();

        worker.handle(client.proxy(), job(Map.of()));

        assertThat(client.failed).isTrue();
        assertThat(client.failRetries).isEqualTo(2);
        assertThat(client.errorMessage).contains(ProcessConstants.VAR_EXECUTION_PAPER_ID);
    }

    private ActivatedJob job(Map<String, Object> variables) {
        return (ActivatedJob) Proxy.newProxyInstance(
                ActivatedJob.class.getClassLoader(),
                new Class<?>[]{ActivatedJob.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getKey" -> 500L;
                    case "getProcessInstanceKey" -> 700L;
                    case "getRetries" -> 3;
                    case "getVariablesAsMap" -> variables;
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private TaxUnitRepository taxUnitRepository(Long taxOfficeId, Long generalDirectorateId) {
        return (TaxUnitRepository) Proxy.newProxyInstance(
                TaxUnitRepository.class.getClassLoader(),
                new Class<?>[]{TaxUnitRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findTaxAffairsOfficeIdByTaxUnitId" -> Optional.of(taxOfficeId);
                    case "findGeneralDirectorateIdByTaxUnitId" -> Optional.of(generalDirectorateId);
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private ExecutionPaperRepository executionPaperRepositoryByDebtPaperCode(
            String debtPaperUniqueCode,
            Long executionPaperId) {
        return (ExecutionPaperRepository) Proxy.newProxyInstance(
                ExecutionPaperRepository.class.getClassLoader(),
                new Class<?>[]{ExecutionPaperRepository.class},
                (proxy, method, args) -> {
                    if ("findByFinalizedDebtPaperDebtPaperUniqueCode".equals(method.getName())
                            && debtPaperUniqueCode.equals(args[0])) {
                        return Optional.of(ExecutionPaper.builder().id(executionPaperId).build());
                    }
                    return defaultValue(method.getReturnType());
                }
        );
    }

    private DebtPaperIssuanceRequest sampleDebtPaper() {
        return new DebtPaperIssuanceRequest(
                "DEBT-1001",
                "2026-07-18",
                "NOTICE-1",
                "قطعی",
                "VAT",
                "1405",
                "SPRING",
                "A01",
                "فعالیت نمونه",
                700L,
                70L,
                7L,
                null,
                new DebtPaperIssuanceRequest.TaxpayerInfo(
                        "NATURAL",
                        "0012345678",
                        null,
                        "علی",
                        "تهرانی",
                        "حسن",
                        "1370-01-01",
                        "1234567890",
                        "تهران",
                        null,
                        null,
                        null,
                        null
                ),
                new DebtPaperIssuanceRequest.DebtInfo(
                        1000d,
                        200d,
                        50d,
                        25d,
                        0d,
                        10d,
                        100d,
                        0d,
                        0d,
                        1185d,
                        0d,
                        0d,
                        0d
                ),
                java.util.List.of()
        );
    }

    private DebtPaperIssuanceRequest sampleLegalDebtPaper() {
        DebtPaperIssuanceRequest source = sampleDebtPaper();
        return new DebtPaperIssuanceRequest(
                source.debtPaperUniqueCode(),
                source.debtPaperIssueDate(),
                source.notificationId(),
                source.finalizationReason(),
                source.taxSource(),
                source.performanceYear(),
                source.performancePeriod(),
                source.activityTypeCode(),
                source.activityTypeTitle(),
                source.mainTaxBureauId(),
                source.taxBureauId(),
                source.taxUnitId(),
                source.sendingUnitId(),
                new DebtPaperIssuanceRequest.TaxpayerInfo(
                        "LEGAL",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "1234567890",
                        "تهران",
                        "14001234567",
                        "12345",
                        "1390-01-01",
                        "شرکت نمونه"
                ),
                source.debtInfo(),
                source.relatedPersons()
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
        if (type == double.class) {
            return 0d;
        }
        if (type == float.class) {
            return 0f;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == char.class) {
            return '\0';
        }
        return null;
    }

    private static class CamundaJobClientStub {
        boolean completed;
        boolean failed;
        Map<String, Object> completeVariables = Map.of();
        int failRetries;
        String errorMessage;

        JobClient proxy() {
            return (JobClient) Proxy.newProxyInstance(
                    JobClient.class.getClassLoader(),
                    new Class<?>[]{JobClient.class},
                    (proxy, method, args) -> {
                        if ("newCompleteCommand".equals(method.getName())) {
                            return commandProxy(method.getReturnType(), true);
                        }
                        if ("newFailCommand".equals(method.getName())) {
                            return commandProxy(method.getReturnType(), false);
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
        }

        private Object commandProxy(Class<?> commandType, boolean completeCommand) {
            return Proxy.newProxyInstance(
                    commandType.getClassLoader(),
                    new Class<?>[]{commandType},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "variables" -> {
                            if (args != null && args.length > 0 && args[0] instanceof Map<?, ?> map) {
                                completeVariables = map.entrySet().stream()
                                        .collect(java.util.stream.Collectors.toMap(
                                                entry -> String.valueOf(entry.getKey()),
                                                Map.Entry::getValue
                                        ));
                            }
                            yield commandProxy(method.getReturnType(), completeCommand);
                        }
                        case "retries" -> {
                            failRetries = (Integer) args[0];
                            yield commandProxy(method.getReturnType(), completeCommand);
                        }
                        case "errorMessage" -> {
                            errorMessage = (String) args[0];
                            yield commandProxy(method.getReturnType(), completeCommand);
                        }
                        case "send" -> {
                            if (completeCommand) {
                                completed = true;
                            } else {
                                failed = true;
                            }
                            yield camundaFuture();
                        }
                        default -> method.getReturnType().isInterface()
                                ? commandProxy(method.getReturnType(), completeCommand)
                                : defaultValue(method.getReturnType());
                    }
            );
        }

        private Object camundaFuture() {
            return Proxy.newProxyInstance(
                    io.camunda.client.api.CamundaFuture.class.getClassLoader(),
                    new Class<?>[]{io.camunda.client.api.CamundaFuture.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "join" -> null;
                        case "isDone" -> true;
                        case "isCancelled" -> false;
                        case "cancel" -> false;
                        case "get" -> null;
                        default -> method.getReturnType().isInterface()
                                ? Proxy.newProxyInstance(
                                method.getReturnType().getClassLoader(),
                                new Class<?>[]{method.getReturnType()},
                                (nestedProxy, nestedMethod, nestedArgs) -> defaultValue(nestedMethod.getReturnType())
                        )
                                : defaultValue(method.getReturnType());
                    }
            );
        }
    }

    private static class StubRuleEngineService extends RuleEngineService {
        private final RuleEvaluationResult result;
        private DebtPaperIssuanceRequest evaluatedRequest;

        private StubRuleEngineService(RuleEvaluationResult result) {
            super(null, new ObjectMapper());
            this.result = result;
        }

        @Override
        public RuleEvaluationResult evaluate(DebtPaperIssuanceRequest request) {
            evaluatedRequest = request;
            return result;
        }
    }

    private static class StubExecutionPaperService extends ExecutionPaperService {
        private final ExecutionPaper executionPaper;
        private DebtPaperIssuanceRequest createdRequest;

        private StubExecutionPaperService(ExecutionPaper executionPaper) {
            super(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
            this.executionPaper = executionPaper;
        }

        @Override
        public ExecutionPaper createFromRequest(DebtPaperIssuanceRequest request) {
            createdRequest = request;
            return executionPaper;
        }
    }

    private static class StubIssuanceRequestService extends IssuanceRequestService {
        private Long rejectedRequestId;
        private String rejectionReason;

        private StubIssuanceRequestService() {
            super(null, new ObjectMapper());
        }

        @Override
        public void markAsRejected(Long requestId, String rejectionReason, Long ruleId) {
            this.rejectedRequestId = requestId;
            this.rejectionReason = rejectionReason;
        }
    }

    private static class StubFakeSanimInformService extends FakeSanimInformService {
        private Long issuanceRejectionRequestId;
        private Long executionPaperId;

        private StubFakeSanimInformService() {
            super(null, null, null, null, new ObjectMapper());
        }

        @Override
        public com.mparsict.vosolmaliat.application.dto.response.FakeSanimInformResponse sendExecutionPaperId(
                Long executionPaperId) {
            this.executionPaperId = executionPaperId;
            return null;
        }

        @Override
        public com.mparsict.vosolmaliat.application.dto.response.FakeSanimInformResponse sendIssuanceRejection(
                Long issuanceRequestId,
                String rejectionReasonCode,
                String rejectionReasonDescription,
                String restrictionRemovalDate) {
            this.issuanceRejectionRequestId = issuanceRequestId;
            return null;
        }
    }

    private static class StubFakeOfficialGazetteService extends FakeOfficialGazetteService {
        private String requestId;
        private Long executionPaperId;
        private String legalPersonNationalId;
        private String debtPaperDate;

        private StubFakeOfficialGazetteService() {
            super(null, null, null, new ObjectMapper(), null, null);
        }

        @Override
        public com.mparsict.vosolmaliat.application.dto.response.FakeOfficialGazetteResponse send(
                String requestId,
                Long executionPaperId,
                String legalPersonNationalId,
                String debtPaperDate) {
            this.requestId = requestId;
            this.executionPaperId = executionPaperId;
            this.legalPersonNationalId = legalPersonNationalId;
            this.debtPaperDate = debtPaperDate;
            return null;
        }
    }

    private static class StubInformProcessService extends InformProcessService {
        private Map<String, Object> checkInformResult = Map.of();
        private Long checkedExecutionPaperId;
        private Long sentToElectronicNotificationExecutionPaperId;
        private Long updatedExecutionPaperId;
        private Boolean updatedCanInform;
        private String updatedInformDate;
        private String updatedRejectionCode;
        private String updatedRejectionDesc;
        private String reInformDate;
        private Long reInformExecutionPaperId;
        private String reInformRestrictionRemovalDate;
        private String nextDebtInquiryDate;
        private Long nextInquiryExecutionPaperId;
        private String nextInquiryInformDate;
        private Long sanimExecutionPaperId;
        private String sanimInformDate;

        private StubInformProcessService() {
            super(null, null, null, null, null, null, null);
        }

        @Override
        public Map<String, Object> checkInformPossibility(Long executionPaperId) {
            checkedExecutionPaperId = executionPaperId;
            return checkInformResult;
        }

        @Override
        public void markSentToElectronicNotification(Long executionPaperId) {
            sentToElectronicNotificationExecutionPaperId = executionPaperId;
        }

        @Override
        public void updateInformStatus(Long executionPaperId, Boolean canInform, String informDate, String rejectionCode, String rejectionDesc) {
            updatedExecutionPaperId = executionPaperId;
            updatedCanInform = canInform;
            updatedInformDate = informDate;
            updatedRejectionCode = rejectionCode;
            updatedRejectionDesc = rejectionDesc;
        }

        @Override
        public String setReInformDate(Long executionPaperId, String restrictionRemovalDate) {
            reInformExecutionPaperId = executionPaperId;
            reInformRestrictionRemovalDate = restrictionRemovalDate;
            return reInformDate;
        }

        @Override
        public String setNextDebtInquiryDate(Long executionPaperId, String informDate) {
            nextInquiryExecutionPaperId = executionPaperId;
            nextInquiryInformDate = informDate;
            return nextDebtInquiryDate;
        }

        @Override
        public void sendInformDateToSanim(Long executionPaperId, String informDate) {
            sanimExecutionPaperId = executionPaperId;
            sanimInformDate = informDate;
        }
    }
}
