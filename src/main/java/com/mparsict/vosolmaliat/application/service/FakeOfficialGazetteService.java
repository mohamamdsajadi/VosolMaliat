package com.mparsict.vosolmaliat.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.integration.CompanyRegistryRelatedPersonData;
import com.mparsict.vosolmaliat.application.dto.response.FakeOfficialGazetteResponse;
import com.mparsict.vosolmaliat.application.service.security.AuditLogService;
import com.mparsict.vosolmaliat.common.exception.BusinessException;
import com.mparsict.vosolmaliat.domain.model.integration.FakeOfficialGazetteRequest;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperRepository;
import com.mparsict.vosolmaliat.domain.repository.FakeOfficialGazetteRequestRepository;
import com.mparsict.vosolmaliat.infrastructure.camunda.service.CamundaMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FakeOfficialGazetteService {

    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_RESPONDED = "RESPONDED";

    private final FakeOfficialGazetteRequestRepository repository;
    private final FakeCompanyRegistryService fakeCompanyRegistryService;
    private final CamundaMessageService camundaMessageService;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;
    private final ExecutionPaperRepository executionPaperRepository;

    @Transactional
    public FakeOfficialGazetteResponse send(
            String requestId,
            Long executionPaperId,
            String legalPersonNationalId,
            String debtPaperDate) {
        FakeOfficialGazetteRequest request = repository.findByRequestId(requestId)
                .orElseGet(FakeOfficialGazetteRequest::new);
        boolean isNew = request.getId() == null;
        request.setRequestId(requestId);
        request.setExecutionPaperId(executionPaperId);
        request.setLegalPersonNationalId(legalPersonNationalId);
        request.setDebtPaperDate(debtPaperDate);
        request.setStatus(STATUS_SENT);
        request.setSentAt(LocalDateTime.now().toString());
        request.setErrorMessage(null);
        FakeOfficialGazetteRequest saved = repository.save(request);
        if (isNew) {
            auditLogService.record(
                    null,
                    "OFFICIAL_GAZETTE_REQUEST_SENT",
                    "EXECUTION_PAPER",
                    executionPaperId.toString(),
                    null,
                    requestId
            );
        }
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<FakeOfficialGazetteResponse> list() {
        return repository.findAllByOrderByIdDesc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CompanyRegistryRelatedPersonData> preview(Long id) {
        FakeOfficialGazetteRequest request = findRequest(id);
        if (request.getResponsePayload() != null && !request.getResponsePayload().isBlank()) {
            return parseRelatedPersons(request.getResponsePayload());
        }
        return fakeCompanyRegistryService.fetchRelatedPersons(request.getLegalPersonNationalId());
    }

    @Transactional
    public FakeOfficialGazetteResponse respond(Long id) {
        return respond(id, null);
    }

    @Transactional
    public FakeOfficialGazetteResponse respond(
            Long id,
            List<CompanyRegistryRelatedPersonData> editedRelatedPersons) {
        FakeOfficialGazetteRequest request = findRequest(id);
        if (STATUS_RESPONDED.equals(request.getStatus())) {
            return toResponse(request);
        }

        List<CompanyRegistryRelatedPersonData> relatedPersons = editedRelatedPersons == null
                ? fakeCompanyRegistryService.fetchRelatedPersons(request.getLegalPersonNationalId())
                : List.copyOf(editedRelatedPersons);
        String responsePayload;
        try {
            responsePayload = objectMapper.writeValueAsString(relatedPersons);
            request.setResponsePayload(responsePayload);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(
                    "FAKE_OFFICIAL_GAZETTE_PAYLOAD_ERROR",
                    "خطا در ساخت پاسخ روزنامه رسمی: " + ex.getMessage(),
                    ex
            );
        }
        camundaMessageService.publishOfficialGazetteRelatedPersons(
                request.getRequestId(),
                relatedPersons
        );
        request.setStatus(STATUS_RESPONDED);
        request.setRespondedAt(LocalDateTime.now().toString());
        FakeOfficialGazetteRequest saved = repository.save(request);
        auditLogService.record(
                null,
                "OFFICIAL_GAZETTE_RESPONSE_SENT",
                "EXECUTION_PAPER",
                request.getExecutionPaperId().toString(),
                null,
                "تعداد اشخاص پاسخ: " + relatedPersons.size()
        );
        return toResponse(saved);
    }

    private FakeOfficialGazetteRequest findRequest(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "FAKE_OFFICIAL_GAZETTE_REQUEST_NOT_FOUND",
                        "درخواست روزنامه رسمی یافت نشد"
                ));
    }

    private List<CompanyRegistryRelatedPersonData> parseRelatedPersons(String payload) {
        try {
            return objectMapper.readValue(
                    payload,
                    new TypeReference<List<CompanyRegistryRelatedPersonData>>() {
                    }
            );
        } catch (JsonProcessingException ex) {
            throw new BusinessException(
                    "FAKE_OFFICIAL_GAZETTE_PAYLOAD_ERROR",
                    "خطا در خواندن پیش‌نمایش روزنامه رسمی: " + ex.getMessage(),
                    ex
            );
        }
    }

    private FakeOfficialGazetteResponse toResponse(FakeOfficialGazetteRequest request) {
        return new FakeOfficialGazetteResponse(
                request.getId(),
                request.getRequestId(),
                request.getExecutionPaperId(),
                executionPaperCode(request.getExecutionPaperId()),
                request.getLegalPersonNationalId(),
                request.getDebtPaperDate(),
                request.getStatus(),
                request.getSentAt(),
                request.getRespondedAt(),
                countRelatedPersons(request.getResponsePayload()),
                request.getErrorMessage()
        );
    }

    private String executionPaperCode(Long executionPaperId) {
        if (executionPaperId == null) {
            return null;
        }
        return executionPaperRepository.findById(executionPaperId)
                .map(executionPaper -> executionPaper.getExecutionPaperUniqueCode())
                .orElse(null);
    }

    private int countRelatedPersons(String payload) {
        if (payload == null || payload.isBlank()) {
            return 0;
        }
        try {
            return objectMapper.readValue(
                    payload,
                    new TypeReference<List<CompanyRegistryRelatedPersonData>>() {
                    }
            ).size();
        } catch (JsonProcessingException ignored) {
            return 0;
        }
    }
}
