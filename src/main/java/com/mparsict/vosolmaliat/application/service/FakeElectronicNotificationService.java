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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FakeElectronicNotificationService {

    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final ZoneId TEHRAN = ZoneId.of("Asia/Tehran");

    private final FakeElectronicNotificationRequestRepository repository;
    private final ExecutionPaperRepository executionPaperRepository;
    private final ExecutionPaperService executionPaperService;
    private final CamundaMessageService camundaMessageService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

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

    private FakeElectronicNotificationResponse confirmRequest(FakeElectronicNotificationRequest request, String informDate) {
        if (STATUS_CONFIRMED.equals(request.getStatus())) {
            return toResponse(request);
        }
        String normalizedInformDate = normalizeInformDateTime(informDate);
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

    private String normalizeInformDateTime(String informDate) {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        if (informDate == null || informDate.isBlank()) {
            return now.toString();
        }

        String raw = informDate.trim();
        String normalized = raw.contains("T") ? raw : raw.replace(" ", "T");
        try {
            return OffsetDateTime.parse(normalized)
                    .atZoneSameInstant(TEHRAN)
                    .toLocalDateTime()
                    .withSecond(0)
                    .withNano(0)
                    .toString();
        } catch (Exception ignored) {
            // Fall through to local date-time or date-only parsing.
        }
        try {
            return LocalDateTime.parse(normalized)
                    .withSecond(0)
                    .withNano(0)
                    .toString();
        } catch (Exception ignored) {
            // Fall through to legacy date-only parsing.
        }
        try {
            return LocalDate.parse(raw.substring(0, Math.min(raw.length(), 10))).atTime(now.toLocalTime()).toString();
        } catch (Exception ignored) {
            return now.toString();
        }
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
                request.getInformDate(),
                request.getConfirmedAt(),
                request.getCallbackDeliveredAt(),
                request.getErrorMessage()
        );
    }
}
