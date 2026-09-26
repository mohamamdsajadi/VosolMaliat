package com.mparsict.vosolmaliat.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.integration.CompanyRegistryRelatedPersonData;
import com.mparsict.vosolmaliat.application.dto.response.FakeRestrictionResponse;
import com.mparsict.vosolmaliat.application.service.security.AuditLogService;
import com.mparsict.vosolmaliat.common.exception.BusinessException;
import com.mparsict.vosolmaliat.domain.model.integration.FakeRestrictionRequest;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperRepository;
import com.mparsict.vosolmaliat.domain.repository.FakeRestrictionRequestRepository;
import com.mparsict.vosolmaliat.infrastructure.camunda.ProcessConstants;
import com.mparsict.vosolmaliat.infrastructure.camunda.service.CamundaMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FakeRestrictionService {

    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_CONFIRMED = "CONFIRMED";

    private final FakeRestrictionRequestRepository repository;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;
    private final ExecutionPaperRepository executionPaperRepository;
    private final CamundaMessageService camundaMessageService;

    @Transactional
    public FakeRestrictionResponse send(
            String officialGazetteRequestId,
            Long executionPaperId,
            List<CompanyRegistryRelatedPersonData> relatedPersons) {
        try {
            FakeRestrictionRequest request = repository
                    .findByOfficialGazetteRequestId(officialGazetteRequestId)
                    .orElseGet(FakeRestrictionRequest::new);
            boolean isNew = request.getId() == null;
            request.setOfficialGazetteRequestId(officialGazetteRequestId);
            request.setExecutionPaperId(executionPaperId);
            request.setStatus(STATUS_SENT);
            request.setRelatedPersonCount(relatedPersons.size());
            request.setPayload(objectMapper.writeValueAsString(relatedPersons));
            request.setSentAt(LocalDateTime.now().toString());
            request.setConfirmedAt(null);
            request.setErrorMessage(null);
            FakeRestrictionRequest saved = repository.save(request);
            if (isNew) {
                auditLogService.record(
                        null,
                        "RESTRICTION_REQUEST_SENT",
                        "EXECUTION_PAPER",
                        executionPaperId.toString(),
                        null,
                        "تعداد اشخاص ارسالی: " + relatedPersons.size()
                );
            }
            return toResponse(saved);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(
                    "FAKE_RESTRICTION_PAYLOAD_ERROR",
                    "خطا در ساخت درخواست اعمال محدودیت: " + ex.getMessage(),
                    ex
            );
        }
    }

    @Transactional(readOnly = true)
    public List<FakeRestrictionResponse> list() {
        return repository.findAllByOrderByIdDesc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public FakeRestrictionResponse confirm(Long id) {
        return confirm(id, null);
    }

    @Transactional
    public FakeRestrictionResponse confirm(
            Long id,
            List<CompanyRegistryRelatedPersonData> editedRelatedPersons) {
        FakeRestrictionRequest request = repository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "FAKE_RESTRICTION_REQUEST_NOT_FOUND",
                        "درخواست اعمال محدودیت یافت نشد"
                ));
        if (editedRelatedPersons != null) {
            try {
                request.setPayload(objectMapper.writeValueAsString(editedRelatedPersons));
                request.setRelatedPersonCount(editedRelatedPersons.size());
            } catch (JsonProcessingException ex) {
                throw new BusinessException(
                        "FAKE_RESTRICTION_PAYLOAD_ERROR",
                        "خطا در ذخیره پیش‌نمایش محدودیت: " + ex.getMessage(),
                        ex
                );
            }
        }
        request.setStatus(STATUS_CONFIRMED);
        request.setConfirmedAt(LocalDateTime.now().toString());
        FakeRestrictionRequest saved = repository.save(request);
        publishConfirmationAfterCommit(ProcessConstants.restrictionRequestId(
                saved.getOfficialGazetteRequestId()
        ));
        auditLogService.record(
                null,
                "RESTRICTION_REQUEST_CONFIRMED",
                "EXECUTION_PAPER",
                request.getExecutionPaperId().toString(),
                null,
                "تعداد اشخاص تاییدشده: " + request.getRelatedPersonCount()
        );
        return toResponse(saved);
    }

    private void publishConfirmationAfterCommit(String restrictionRequestId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            camundaMessageService.publishRestrictionsApplied(restrictionRequestId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                camundaMessageService.publishRestrictionsApplied(restrictionRequestId);
            }
        });
    }

    private FakeRestrictionResponse toResponse(FakeRestrictionRequest request) {
        return new FakeRestrictionResponse(
                request.getId(),
                request.getOfficialGazetteRequestId(),
                request.getExecutionPaperId(),
                executionPaperCode(request.getExecutionPaperId()),
                request.getStatus(),
                request.getRelatedPersonCount(),
                request.getPayload(),
                request.getSentAt(),
                request.getConfirmedAt(),
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
}
