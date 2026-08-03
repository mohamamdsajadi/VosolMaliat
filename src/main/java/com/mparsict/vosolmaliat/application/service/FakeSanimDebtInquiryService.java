package com.mparsict.vosolmaliat.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.response.FakeSanimDebtInquiryResponse;
import com.mparsict.vosolmaliat.application.dto.response.SanimDebtStatusResponse;
import com.mparsict.vosolmaliat.application.dto.response.SanimTaxpayerOptionResponse;
import com.mparsict.vosolmaliat.application.service.security.AuditLogService;
import com.mparsict.vosolmaliat.common.exception.BusinessException;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaper;
import com.mparsict.vosolmaliat.domain.model.integration.FakeSanimDebtInquiryRequest;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperRepository;
import com.mparsict.vosolmaliat.domain.repository.FakeSanimDebtInquiryRequestRepository;
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
public class FakeSanimDebtInquiryService {

    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_RESPONDED = "RESPONDED";

    private final FakeSanimDebtInquiryRequestRepository repository;
    private final ExecutionPaperRepository executionPaperRepository;
    private final SanimAuditService sanimAuditService;
    private final CamundaMessageService camundaMessageService;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;

    @Transactional
    public FakeSanimDebtInquiryResponse send(String requestId, Long executionPaperId) {
        return repository.findByRequestId(requestId)
                .map(request -> {
                    if (!executionPaperId.equals(request.getExecutionPaperId())) {
                        throw new BusinessException(
                                "SANIM_DEBT_INQUIRY_CORRELATION_CONFLICT",
                                "شناسه همبستگی استعلام بدهی قبلاً برای برگ اجرای دیگری استفاده شده است"
                        );
                    }
                    return toResponse(request);
                })
                .orElseGet(() -> create(requestId, executionPaperId));
    }

    @Transactional(readOnly = true)
    public List<FakeSanimDebtInquiryResponse> list() {
        return repository.findAllByOrderByIdDesc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public FakeSanimDebtInquiryResponse respond(Long id) {
        FakeSanimDebtInquiryRequest request = repository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        "FAKE_SANIM_DEBT_INQUIRY_NOT_FOUND",
                        "درخواست استعلام بدهی سنیم یافت نشد"
                ));

        SanimDebtStatusResponse status;
        if (STATUS_RESPONDED.equals(request.getStatus()) && request.getResponsePayload() != null) {
            status = readStatus(request.getResponsePayload());
        } else {
            status = sanimAuditService.inquire(request.getTaxpayerType(), request.getTaxpayerIdentifier());
            request.setResponsePayload(writeStatus(status));
            request.setStatus(STATUS_RESPONDED);
            request.setRespondedAt(LocalDateTime.now());
            request.setErrorMessage(null);
            repository.save(request);
            auditLogService.record(
                    null,
                    "SANIM_DEBT_STATUS_RESPONDED",
                    "EXECUTION_PAPER",
                    request.getExecutionPaperId().toString(),
                    null,
                    request.getRequestId()
            );
        }

        publishAfterCommit(request.getRequestId(), status);
        return toResponse(request);
    }

    private FakeSanimDebtInquiryResponse create(String requestId, Long executionPaperId) {
        ExecutionPaper executionPaper = executionPaperRepository.findById(executionPaperId)
                .orElseThrow(() -> new BusinessException(
                        "EXECUTION_PAPER_NOT_FOUND",
                        "برگ اجرا یافت نشد"
                ));
        if (executionPaper.getTaxpayerId() == null) {
            throw new BusinessException(
                    "EXECUTION_PAPER_TAXPAYER_MISSING",
                    "مؤدی برگ اجرا مشخص نشده است"
            );
        }
        SanimTaxpayerOptionResponse taxpayer = sanimAuditService.taxpayer(executionPaper.getTaxpayerId());
        FakeSanimDebtInquiryRequest saved = repository.save(FakeSanimDebtInquiryRequest.builder()
                .requestId(requestId)
                .executionPaperId(executionPaperId)
                .taxpayerType(taxpayer.taxpayerType())
                .taxpayerIdentifier(taxpayer.identifier())
                .status(STATUS_SENT)
                .sentAt(LocalDateTime.now())
                .build());
        auditLogService.record(
                null,
                "SANIM_DEBT_INQUIRY_SENT",
                "EXECUTION_PAPER",
                executionPaperId.toString(),
                null,
                requestId
        );
        return toResponse(saved);
    }

    private void publishAfterCommit(String requestId, SanimDebtStatusResponse status) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            camundaMessageService.publishTaxpayerDebt(requestId, status);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                camundaMessageService.publishTaxpayerDebt(requestId, status);
            }
        });
    }

    private FakeSanimDebtInquiryResponse toResponse(FakeSanimDebtInquiryRequest request) {
        return new FakeSanimDebtInquiryResponse(
                request.getId(),
                request.getRequestId(),
                request.getExecutionPaperId(),
                executionPaperRepository.findById(request.getExecutionPaperId())
                        .map(ExecutionPaper::getExecutionPaperUniqueCode)
                        .orElse(null),
                request.getTaxpayerType(),
                request.getTaxpayerIdentifier(),
                request.getStatus(),
                request.getSentAt(),
                request.getRespondedAt(),
                readStatus(request.getResponsePayload()),
                request.getErrorMessage()
        );
    }

    private String writeStatus(SanimDebtStatusResponse status) {
        try {
            return objectMapper.writeValueAsString(status);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("FAKE_SANIM_DEBT_RESPONSE_ERROR", "خطا در ساخت پاسخ سنیم", ex);
        }
    }

    private SanimDebtStatusResponse readStatus(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(payload, SanimDebtStatusResponse.class);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("FAKE_SANIM_DEBT_RESPONSE_ERROR", "خطا در خواندن پاسخ سنیم", ex);
        }
    }
}
