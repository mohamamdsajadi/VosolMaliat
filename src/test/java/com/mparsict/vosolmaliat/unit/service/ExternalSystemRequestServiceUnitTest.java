package com.mparsict.vosolmaliat.unit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.response.ExternalSystemRequestResponse;
import com.mparsict.vosolmaliat.application.service.ExternalSystemRequestService;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaper;
import com.mparsict.vosolmaliat.domain.model.integration.FakeElectronicNotificationRequest;
import com.mparsict.vosolmaliat.domain.model.integration.FakeOfficialGazetteRequest;
import com.mparsict.vosolmaliat.domain.model.integration.FakeRestrictionRequest;
import com.mparsict.vosolmaliat.domain.model.integration.FakeSanimInformRequest;
import com.mparsict.vosolmaliat.domain.repository.FakeElectronicNotificationRequestRepository;
import com.mparsict.vosolmaliat.domain.repository.FakeOfficialGazetteRequestRepository;
import com.mparsict.vosolmaliat.domain.repository.FakeRestrictionRequestRepository;
import com.mparsict.vosolmaliat.domain.repository.FakeSanimInformRequestRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalSystemRequestServiceUnitTest {

    @Test
    void returnsPersistedOutboundBodiesWithUnderstandableMetadata() {
        ExecutionPaper executionPaper = ExecutionPaper.builder().id(99L).build();
        FakeElectronicNotificationRequest notification = FakeElectronicNotificationRequest.builder()
                .id(1L).executionPaper(executionPaper).externalTrackingCode("EB-100")
                .status("SENT").sentAt("2026-08-02T10:00:00")
                .payload("{\"executionPaper\":{\"uniqueCode\":\"EP-99\"}}")
                .build();
        FakeOfficialGazetteRequest gazette = FakeOfficialGazetteRequest.builder()
                .id(2L).requestId("official-gazette-700").executionPaperId(99L)
                .legalPersonNationalId("14001234567").debtPaperDate("1405-01-01")
                .status("RESPONDED").sentAt("2026-08-02T10:01:00")
                .build();
        FakeRestrictionRequest restriction = FakeRestrictionRequest.builder()
                .id(3L).officialGazetteRequestId("official-gazette-700").executionPaperId(99L)
                .status("CONFIRMED").sentAt("2026-08-02T10:02:00")
                .payload("[{\"personType\":\"NATURAL\",\"nationalCode\":\"0012345678\"}]")
                .build();
        FakeSanimInformRequest sanim = FakeSanimInformRequest.builder()
                .id(4L).executionPaper(executionPaper).messageType("EXECUTION_PAPER_ID")
                .status("CONFIRMED").sentAt("2026-08-02T10:03:00")
                .payload("{\"messageType\":\"EXECUTION_PAPER_ID\",\"executionPaperId\":99}")
                .build();
        ExternalSystemRequestService service = new ExternalSystemRequestService(
                repository(FakeElectronicNotificationRequestRepository.class, List.of(notification)),
                repository(FakeOfficialGazetteRequestRepository.class, List.of(gazette)),
                repository(FakeRestrictionRequestRepository.class, List.of(restriction)),
                repository(FakeSanimInformRequestRepository.class, List.of(sanim)),
                new ObjectMapper()
        );

        List<ExternalSystemRequestResponse> result = service.findByExecutionPaperId(99L);

        assertThat(result).extracting(ExternalSystemRequestResponse::systemCode)
                .containsExactly("SANIM", "COMPANY_REGISTRY", "OFFICIAL_GAZETTE", "ELECTRONIC_NOTIFICATION");
        assertThat(result.getFirst().requestBody()).isEqualTo(Map.of(
                "messageType", "EXECUTION_PAPER_ID",
                "executionPaperId", 99
        ));
        assertThat(result.get(1).requestBody()).isInstanceOfSatisfying(Map.class, body -> {
            assertThat(body).containsEntry("officialGazetteRequestId", "official-gazette-700");
            assertThat(body.get("relatedPersons")).isEqualTo(List.of(Map.of(
                    "personType", "NATURAL",
                    "nationalCode", "0012345678"
            )));
        });
        assertThat(result.getLast().requestBody()).isEqualTo(Map.of(
                "executionPaper", Map.of("uniqueCode", "EP-99")
        ));
    }

    @SuppressWarnings("unchecked")
    private <T> T repository(Class<T> type, List<?> records) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> method.getName().startsWith("findAllByExecutionPaper")
                        ? records
                        : defaultValue(method.getReturnType()));
    }

    private Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == long.class) return 0L;
        if (type == int.class) return 0;
        return null;
    }
}
