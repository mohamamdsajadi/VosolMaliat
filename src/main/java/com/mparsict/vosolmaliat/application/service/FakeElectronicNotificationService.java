package com.mparsict.vosolmaliat.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.response.ExecutionPaperDetailResponse;
import com.mparsict.vosolmaliat.application.dto.response.FakeElectronicNotificationResponse;
import com.mparsict.vosolmaliat.application.service.security.AuditLogService;
import com.mparsict.vosolmaliat.common.exception.BusinessException;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaper;
import com.mparsict.vosolmaliat.domain.model.integration.FakeElectronicNotificationRequest;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperRepository;
import com.mparsict.vosolmaliat.domain.repository.FakeElectronicNotificationRequestRepository;
import com.mparsict.vosolmaliat.infrastructure.camunda.service.CamundaMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FakeElectronicNotificationService {

    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_CONFIRMED = "CONFIRMED";

    private final FakeElectronicNotificationRequestRepository repository;
    private final ExecutionPaperRepository executionPaperRepository;
    private final ExecutionPaperService executionPaperService;
    private final CamundaMessageService camundaMessageService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Value("${app.fake-electronic-notification.auto-confirm-delay:PT30S}")
    private Duration autoConfirmDelay;

    @Transactional
    public FakeElectronicNotificationResponse send(Long executionPaperId) {
        ExecutionPaper executionPaper = executionPaperRepository.findById(executionPaperId)
                .orElseThrow(() -> new BusinessException("EXECUTION_PAPER_NOT_FOUND", "برگ اجرا یافت نشد"));

        LocalDateTime now = LocalDateTime.now();
        FakeElectronicNotificationRequest request = FakeElectronicNotificationRequest.builder()
                .executionPaper(executionPaper)
                .externalTrackingCode("EB-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .status(STATUS_SENT)
                .payload(toPayload(executionPaperId))
                .sentAt(now.toString())
                .autoConfirmAt(now.plus(autoConfirmDelay).toString())
                .build();
        FakeElectronicNotificationRequest saved = repository.save(request);
        auditLogService.record(null, "INFORM_SENT_TO_ELECTRONIC_NOTIFICATION", "EXECUTION_PAPER",
                executionPaperId.toString(), null, saved.getExternalTrackingCode());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<FakeElectronicNotificationResponse> list() {
        return repository.findAllByOrderByIdDesc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public FakeElectronicNotificationResponse confirm(Long requestId, String informDate) {
        FakeElectronicNotificationRequest request = repository.findById(requestId)
                .orElseThrow(() -> new BusinessException("FAKE_EB_REQUEST_NOT_FOUND", "درخواست ابلاغ الکترونیک یافت نشد"));
        return confirmRequest(request, informDate);
    }

    @Transactional
    public FakeElectronicNotificationResponse confirmByExecutionPaper(Long executionPaperId, String informDate) {
        FakeElectronicNotificationRequest request = repository.findFirstByExecutionPaper_IdOrderByIdDesc(executionPaperId)
                .orElseThrow(() -> new BusinessException("FAKE_EB_REQUEST_NOT_FOUND", "درخواست ابلاغ الکترونیک برای این برگ اجرا یافت نشد"));
        return confirmRequest(request, informDate);
    }

    @Scheduled(fixedDelayString = "${app.fake-electronic-notification.auto-confirm-poll-ms:10000}")
    @Transactional
    public void autoConfirmDueRequests() {
        LocalDateTime now = LocalDateTime.now();
        for (FakeElectronicNotificationRequest request : repository.findByStatusOrderByIdAsc(STATUS_SENT)) {
            if (request.getAutoConfirmAt() == null) {
                continue;
            }
            try {
                LocalDateTime autoConfirmAt = LocalDateTime.parse(request.getAutoConfirmAt());
                if (!autoConfirmAt.isAfter(now)) {
                    confirmRequest(request, now.toLocalDate().toString());
                }
            } catch (Exception ex) {
                log.warn("Could not auto-confirm fake EB request id={}: {}", request.getId(), ex.getMessage());
            }
        }
    }

    private FakeElectronicNotificationResponse confirmRequest(FakeElectronicNotificationRequest request, String informDate) {
        if (STATUS_CONFIRMED.equals(request.getStatus())) {
            return toResponse(request);
        }
        String normalizedInformDate = informDate == null || informDate.isBlank()
                ? LocalDateTime.now().toLocalDate().toString()
                : informDate;
        request.setInformDate(normalizedInformDate);
        request.setConfirmedAt(LocalDateTime.now().toString());
        request.setStatus(STATUS_CONFIRMED);
        camundaMessageService.publishExecutionPaperInformed(request.getExecutionPaperId(), normalizedInformDate);
        request.setCallbackDeliveredAt(LocalDateTime.now().toString());
        FakeElectronicNotificationRequest saved = repository.save(request);
        auditLogService.record(null, "ELECTRONIC_NOTIFICATION_CONFIRMED", "EXECUTION_PAPER",
                request.getExecutionPaperId().toString(), null, normalizedInformDate);
        return toResponse(saved);
    }

    private String toPayload(Long executionPaperId) {
        try {
            ExecutionPaperDetailResponse details = executionPaperService.getDetails(executionPaperId);
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("FAKE_EB_PAYLOAD_ERROR", "خطا در ساخت payload ابلاغ الکترونیک: " + ex.getMessage(), ex);
        }
    }

    private FakeElectronicNotificationResponse toResponse(FakeElectronicNotificationRequest request) {
        return new FakeElectronicNotificationResponse(
                request.getId(),
                request.getExecutionPaperId(),
                request.getExecutionPaper() == null ? null : request.getExecutionPaper().getExecutionPaperUniqueCode(),
                request.getExternalTrackingCode(),
                request.getStatus(),
                request.getSentAt(),
                request.getAutoConfirmAt(),
                request.getInformDate(),
                request.getConfirmedAt(),
                request.getCallbackDeliveredAt(),
                request.getErrorMessage()
        );
    }
}
