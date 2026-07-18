package com.mparsict.vosolmaliat.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.response.FakeSanimInformResponse;
import com.mparsict.vosolmaliat.application.service.security.AuditLogService;
import com.mparsict.vosolmaliat.common.exception.BusinessException;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaper;
import com.mparsict.vosolmaliat.domain.model.integration.FakeSanimInformRequest;
import com.mparsict.vosolmaliat.domain.model.rule.ExecutionPaperIssuanceRequest;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperRepository;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperIssuanceRequestRepository;
import com.mparsict.vosolmaliat.domain.repository.FakeSanimInformRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FakeSanimInformService {

    private static final String MESSAGE_TYPE_INFORM_DATE = "INFORM_DATE";
    private static final String MESSAGE_TYPE_ISSUANCE_REJECTION = "ISSUANCE_REJECTION";
    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_CONFIRMED = "CONFIRMED";

    private final FakeSanimInformRequestRepository repository;
    private final ExecutionPaperRepository executionPaperRepository;
    private final ExecutionPaperIssuanceRequestRepository issuanceRequestRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Transactional
    public FakeSanimInformResponse sendInformDate(Long executionPaperId, String informDate) {
        ExecutionPaper executionPaper = executionPaperRepository.findById(executionPaperId)
                .orElseThrow(() -> new BusinessException("EXECUTION_PAPER_NOT_FOUND", "برگ اجرا یافت نشد"));
        FakeSanimInformRequest request = FakeSanimInformRequest.builder()
                .executionPaper(executionPaper)
                .messageType(MESSAGE_TYPE_INFORM_DATE)
                .status(STATUS_SENT)
                .informDate(informDate)
                .payload(toPayload(executionPaper, informDate))
                .sentAt(LocalDateTime.now().toString())
                .build();
        FakeSanimInformRequest saved = repository.save(request);
        auditLogService.record(null, "INFORM_DATE_SENT_TO_SANIM", "EXECUTION_PAPER",
                executionPaperId.toString(), null, informDate);
        return toResponse(saved);
    }

    @Transactional
    public FakeSanimInformResponse sendIssuanceRejection(
            Long issuanceRequestId,
            String rejectionReasonCode,
            String rejectionReasonDescription,
            String restrictionRemovalDate) {
        String debtPaperId = issuanceRequestRepository.findById(issuanceRequestId)
                .map(ExecutionPaperIssuanceRequest::getDebtPaperUniqueCode)
                .orElse(issuanceRequestId == null ? null : issuanceRequestId.toString());
        FakeSanimInformRequest request = FakeSanimInformRequest.builder()
                .messageType(MESSAGE_TYPE_ISSUANCE_REJECTION)
                .debtPaperId(debtPaperId)
                .status(STATUS_SENT)
                .rejectionReasonCode(rejectionReasonCode)
                .rejectionReasonDescription(rejectionReasonDescription)
                .restrictionRemovalDate(restrictionRemovalDate)
                .payload(toRejectionPayload(debtPaperId, rejectionReasonCode, rejectionReasonDescription, restrictionRemovalDate))
                .sentAt(LocalDateTime.now().toString())
                .build();
        FakeSanimInformRequest saved = repository.save(request);
        auditLogService.record(null, "ISSUANCE_REJECTION_SENT_TO_SANIM", "DEBT_PAPER",
                debtPaperId, null, saved.getPayload());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<FakeSanimInformResponse> list() {
        return repository.findAllByOrderByIdDesc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public FakeSanimInformResponse confirm(Long requestId) {
        FakeSanimInformRequest request = repository.findById(requestId)
                .orElseThrow(() -> new BusinessException("FAKE_SANIM_REQUEST_NOT_FOUND", "درخواست سنیم یافت نشد"));
        request.setStatus(STATUS_CONFIRMED);
        request.setConfirmedAt(LocalDateTime.now().toString());
        FakeSanimInformRequest saved = repository.save(request);
        String targetType = MESSAGE_TYPE_ISSUANCE_REJECTION.equals(request.getMessageType()) ? "DEBT_PAPER" : "EXECUTION_PAPER";
        String targetId = MESSAGE_TYPE_ISSUANCE_REJECTION.equals(request.getMessageType())
                ? request.getDebtPaperId()
                : request.getExecutionPaperId() == null ? null : request.getExecutionPaperId().toString();
        auditLogService.record(null, "SANIM_MESSAGE_CONFIRMED", targetType, targetId, null, request.getMessageType());
        return toResponse(saved);
    }

    private String toPayload(ExecutionPaper executionPaper, String informDate) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("messageType", MESSAGE_TYPE_INFORM_DATE);
            payload.put("executionPaperId", executionPaper.getId());
            payload.put("executionPaperCode", executionPaper.getExecutionPaperUniqueCode());
            payload.put("InformDate", informDate);
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("FAKE_SANIM_PAYLOAD_ERROR", "خطا در ساخت payload سنیم: " + ex.getMessage(), ex);
        }
    }

    private String toRejectionPayload(
            String debtPaperId,
            String rejectionReasonCode,
            String rejectionReasonDescription,
            String restrictionRemovalDate) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("messageType", MESSAGE_TYPE_ISSUANCE_REJECTION);
            payload.put("debtPaperId", debtPaperId);
            payload.put("rejectionReasonCode", rejectionReasonCode);
            payload.put("rejectionReasonDescription", rejectionReasonDescription);
            payload.put("restrictionRemovalDate", restrictionRemovalDate);
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("FAKE_SANIM_PAYLOAD_ERROR", "خطا در ساخت payload سنیم: " + ex.getMessage(), ex);
        }
    }

    private FakeSanimInformResponse toResponse(FakeSanimInformRequest request) {
        return new FakeSanimInformResponse(
                request.getId(),
                request.getExecutionPaperId(),
                request.getExecutionPaper() == null ? null : request.getExecutionPaper().getExecutionPaperUniqueCode(),
                request.getMessageType(),
                request.getDebtPaperId(),
                request.getStatus(),
                request.getInformDate(),
                request.getRejectionReasonCode(),
                request.getRejectionReasonDescription(),
                request.getRestrictionRemovalDate(),
                request.getSentAt(),
                request.getConfirmedAt(),
                request.getErrorMessage()
        );
    }
}
