package com.mparsict.vosolmaliat.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.request.DebtPaperIssuanceRequest;
import com.mparsict.vosolmaliat.common.exception.BusinessException;
import com.mparsict.vosolmaliat.common.util.HashUtil;
import com.mparsict.vosolmaliat.domain.model.rule.ExecutionPaperIssuanceRequest;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperIssuanceRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IssuanceRequestService {

    private final ExecutionPaperIssuanceRequestRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public ExecutionPaperIssuanceRequest createAndSave(DebtPaperIssuanceRequest request) {
        ModelRequestValidation.requireRequest(request);
        SanimModelValidation.requireDocument(request.sourceDocument());
        String json;
        try {
            json = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new BusinessException("SERIALIZATION_ERROR", "خطا در تبدیل درخواست به JSON: " + e.getMessage());
        }

        String hash = HashUtil.sha256(json);

        ExecutionPaperIssuanceRequest entity = ExecutionPaperIssuanceRequest.builder()
                .debtPaperUniqueCode(request.debtPaperUniqueCode())
                .payload(json)
                .xml(request.sourceDocument().getXml())
                .hash(hash)
                .build();

        ExecutionPaperIssuanceRequest saved = repository.save(entity);
        log.debug("Saved issuance request id={}, code={}", saved.getId(), saved.getDebtPaperUniqueCode());
        return saved;
    }

    @Transactional(readOnly = true)
    public DebtPaperIssuanceRequest loadRequest(Long requestId) {
        ExecutionPaperIssuanceRequest entity = repository.findById(requestId)
                .orElseThrow(() -> new BusinessException(
                        "ISSUANCE_REQUEST_NOT_FOUND", "درخواست صدور با شناسه " + requestId + " یافت نشد"));
        if (entity.getPayload() == null || entity.getPayload().isBlank()) {
            throw new BusinessException("ISSUANCE_PAYLOAD_MISSING", "بدنه درخواست صدور ذخیره نشده است");
        }
        try {
            DebtPaperIssuanceRequest request = objectMapper.readValue(entity.getPayload(), DebtPaperIssuanceRequest.class);
            if (request == null) throw new BusinessException("ISSUANCE_PAYLOAD_INVALID", "بدنه درخواست صدور خالی است");
            return request;
        } catch (JsonProcessingException exception) {
            throw new BusinessException("ISSUANCE_PAYLOAD_INVALID",
                    "بدنه درخواست صدور قابل خواندن نیست: " + exception.getOriginalMessage(), exception);
        }
    }

    @Transactional
    public void markAsRejected(Long requestId, String rejectionReason, Long ruleId) {
        repository.findById(requestId).ifPresent(req -> {
            repository.save(ExecutionPaperIssuanceRequest.builder()
                    .originalRequestId(requestId).debtPaperUniqueCode(req.getDebtPaperUniqueCode())
                    .payload(req.getPayload()).xml(req.getXml()).hash(req.getHash())
                    .rejectionReason(rejectionReason).rejectionReasonId(ruleId).build());
            log.debug("Marked issuance request id={} as rejected with reason={}", requestId, rejectionReason);
        });
    }

    public Optional<ExecutionPaperIssuanceRequest> findByHash(String hash) {
        return repository.findByHash(hash);
    }
}
