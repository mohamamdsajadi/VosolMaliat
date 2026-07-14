package com.mparsict.vosolmaliat.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.response.FakeSanimInformResponse;
import com.mparsict.vosolmaliat.application.service.security.AuditLogService;
import com.mparsict.vosolmaliat.common.exception.BusinessException;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaper;
import com.mparsict.vosolmaliat.domain.model.integration.FakeSanimInformRequest;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperRepository;
import com.mparsict.vosolmaliat.domain.repository.FakeSanimInformRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FakeSanimInformService {

    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_CONFIRMED = "CONFIRMED";

    private final FakeSanimInformRequestRepository repository;
    private final ExecutionPaperRepository executionPaperRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Transactional
    public FakeSanimInformResponse sendInformDate(Long executionPaperId, String informDate) {
        ExecutionPaper executionPaper = executionPaperRepository.findById(executionPaperId)
                .orElseThrow(() -> new BusinessException("EXECUTION_PAPER_NOT_FOUND", "برگ اجرا یافت نشد"));
        FakeSanimInformRequest request = FakeSanimInformRequest.builder()
                .executionPaper(executionPaper)
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
        auditLogService.record(null, "SANIM_INFORM_DATE_CONFIRMED", "EXECUTION_PAPER",
                request.getExecutionPaperId().toString(), null, request.getInformDate());
        return toResponse(saved);
    }

    private String toPayload(ExecutionPaper executionPaper, String informDate) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "executionPaperId", executionPaper.getId(),
                    "executionPaperCode", executionPaper.getExecutionPaperUniqueCode(),
                    "InformDate", informDate
            ));
        } catch (JsonProcessingException ex) {
            throw new BusinessException("FAKE_SANIM_PAYLOAD_ERROR", "خطا در ساخت payload سنیم: " + ex.getMessage(), ex);
        }
    }

    private FakeSanimInformResponse toResponse(FakeSanimInformRequest request) {
        return new FakeSanimInformResponse(
                request.getId(),
                request.getExecutionPaperId(),
                request.getExecutionPaper() == null ? null : request.getExecutionPaper().getExecutionPaperUniqueCode(),
                request.getStatus(),
                request.getInformDate(),
                request.getSentAt(),
                request.getConfirmedAt(),
                request.getErrorMessage()
        );
    }
}
