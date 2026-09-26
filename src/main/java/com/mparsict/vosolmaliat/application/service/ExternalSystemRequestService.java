package com.mparsict.vosolmaliat.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.response.ExternalSystemRequestResponse;
import com.mparsict.vosolmaliat.domain.model.integration.FakeElectronicNotificationRequest;
import com.mparsict.vosolmaliat.domain.model.integration.FakeOfficialGazetteRequest;
import com.mparsict.vosolmaliat.domain.model.integration.FakeRestrictionRequest;
import com.mparsict.vosolmaliat.domain.model.integration.FakeSanimInformRequest;
import com.mparsict.vosolmaliat.domain.repository.FakeElectronicNotificationRequestRepository;
import com.mparsict.vosolmaliat.domain.repository.FakeOfficialGazetteRequestRepository;
import com.mparsict.vosolmaliat.domain.repository.FakeRestrictionRequestRepository;
import com.mparsict.vosolmaliat.domain.repository.FakeSanimInformRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExternalSystemRequestService {
    private final FakeElectronicNotificationRequestRepository electronicNotificationRepository;
    private final FakeOfficialGazetteRequestRepository officialGazetteRepository;
    private final FakeRestrictionRequestRepository restrictionRepository;
    private final FakeSanimInformRequestRepository sanimRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<ExternalSystemRequestResponse> findByExecutionPaperId(Long executionPaperId) {
        List<ExternalSystemRequestResponse> requests = new ArrayList<>();
        electronicNotificationRepository.findAllByExecutionPaper_IdOrderByIdDesc(executionPaperId)
                .stream().map(this::electronicNotification).forEach(requests::add);
        officialGazetteRepository.findAllByExecutionPaperIdOrderByIdDesc(executionPaperId)
                .stream().map(this::officialGazette).forEach(requests::add);
        restrictionRepository.findAllByExecutionPaperIdOrderByIdDesc(executionPaperId)
                .stream().map(this::restriction).forEach(requests::add);
        sanimRepository.findAllByExecutionPaper_IdOrderByIdDesc(executionPaperId)
                .stream().map(this::sanim).forEach(requests::add);
        return requests.stream()
                .sorted(Comparator.comparing(
                        ExternalSystemRequestResponse::sentAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();
    }

    private ExternalSystemRequestResponse electronicNotification(FakeElectronicNotificationRequest request) {
        return response(
                request.getId(),
                "ELECTRONIC_NOTIFICATION",
                "سامانه ابلاغ الکترونیکی",
                "EXECUTION_PAPER_NOTIFICATION",
                "ارسال برگ اجرا برای ابلاغ",
                request.getStatus(),
                request.getSentAt(),
                request.getConfirmedAt(),
                request.getExternalTrackingCode(),
                parsePayload(request.getPayload())
        );
    }

    private ExternalSystemRequestResponse officialGazette(FakeOfficialGazetteRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("requestId", request.getRequestId());
        body.put("executionPaperId", request.getExecutionPaperId());
        body.put("legalPersonNationalId", request.getLegalPersonNationalId());
        body.put("debtPaperDate", request.getDebtPaperDate());
        return response(
                request.getId(),
                "OFFICIAL_GAZETTE",
                "سامانه روزنامه رسمی",
                "BOARD_MEMBERS_INQUIRY",
                "استعلام اعضای هیئت‌مدیره",
                request.getStatus(),
                request.getSentAt(),
                request.getRespondedAt(),
                request.getRequestId(),
                body
        );
    }

    private ExternalSystemRequestResponse restriction(FakeRestrictionRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("officialGazetteRequestId", request.getOfficialGazetteRequestId());
        body.put("executionPaperId", request.getExecutionPaperId());
        body.put("relatedPersons", parsePayload(request.getPayload()));
        return response(
                request.getId(),
                "COMPANY_REGISTRY",
                "سامانه ثبت شرکت‌ها",
                "RELATED_PERSON_RESTRICTION",
                "درخواست اعمال محدودیت برای اشخاص مرتبط",
                request.getStatus(),
                request.getSentAt(),
                request.getConfirmedAt(),
                request.getOfficialGazetteRequestId(),
                body
        );
    }

    private ExternalSystemRequestResponse sanim(FakeSanimInformRequest request) {
        String typeLabel = switch (request.getMessageType() == null ? "" : request.getMessageType()) {
            case "EXECUTION_PAPER_ID" -> "ارسال شناسه برگ اجرا";
            case "INFORM_DATE" -> "ارسال تاریخ ابلاغ";
            case "ISSUANCE_REJECTION" -> "ارسال نتیجه رد صدور";
            default -> request.getMessageType();
        };
        return response(
                request.getId(),
                "SANIM",
                "سامانه سنیم",
                request.getMessageType(),
                typeLabel,
                request.getStatus(),
                request.getSentAt(),
                request.getConfirmedAt(),
                null,
                parsePayload(request.getPayload())
        );
    }

    private ExternalSystemRequestResponse response(
            Long id,
            String systemCode,
            String systemLabel,
            String requestType,
            String requestTypeLabel,
            String status,
            String sentAt,
            String completedAt,
            String trackingCode,
            Object body) {
        return new ExternalSystemRequestResponse(
                id,
                systemCode,
                systemLabel,
                requestType,
                requestTypeLabel,
                status,
                statusLabel(status),
                sentAt,
                completedAt,
                trackingCode,
                body
        );
    }

    private String statusLabel(String status) {
        return switch (status == null ? "" : status) {
            case "SENT" -> "ارسال‌شده و در انتظار پاسخ";
            case "CONFIRMED" -> "تأییدشده";
            case "RESPONDED" -> "پاسخ دریافت شده";
            case "FAILED" -> "ناموفق";
            default -> status;
        };
    }

    private Object parsePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(payload, Object.class);
        } catch (Exception ignored) {
            return Map.of("rawPayload", payload);
        }
    }
}
