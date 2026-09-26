package com.mparsict.vosolmaliat.application.service;

import com.mparsict.vosolmaliat.application.dto.request.DebtPaperIssuanceRequest;
import com.mparsict.vosolmaliat.application.dto.request.DebtPaperPushRequest;
import com.mparsict.vosolmaliat.common.exception.BusinessException;
import com.mparsict.vosolmaliat.common.exception.DebtPaperPushException;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaper;
import com.mparsict.vosolmaliat.infrastructure.camunda.service.CamundaProcessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DebtPaperPushService {

    private final DebtPaperPushMapper mapper;
    private final DebtPaperPushPersistenceService persistenceService;
    private final CamundaProcessService camundaProcessService;
    private final Environment environment;

    public Long push(DebtPaperPushRequest request, String traceId) {
        log.info("Debt Paper processing started: referenceID={}, traceID={}", request.referenceId(), traceId);
        Long recordId;
        try {
            recordId = persistenceService.receive(request, traceId);
        } catch (DataIntegrityViolationException exception) {
            persistenceService.rejectExistingReference(request.referenceId());
            throw exception;
        }
        return process(recordId, request, traceId, false);
    }

    /**
     * Synchronous test-data path used only by the secured EF03 admin endpoint.
     * Production always keeps the identity gate.
     */
    public Long pushImmediately(DebtPaperPushRequest request, String traceId) {
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            throw new BusinessException("IMMEDIATE_ISSUANCE_DISABLED",
                    "صدور فوری EF03 در محیط production غیرفعال است");
        }
        log.info("Immediate admin EF03 processing started: referenceID={}, traceID={}", request.referenceId(), traceId);
        Long waitingRecordId = persistenceService.waitingRecordId(request.referenceId());
        if (waitingRecordId != null) {
            return process(waitingRecordId, request, traceId, true);
        }
        Long recordId;
        try {
            recordId = persistenceService.receive(request, traceId);
        } catch (DataIntegrityViolationException exception) {
            persistenceService.rejectExistingReference(request.referenceId());
            throw exception;
        }
        return process(recordId, request, traceId, true);
    }

    public Long resume(Long recordId) {
        return process(recordId, persistenceService.sourceRequest(recordId), "identity-" + recordId, false);
    }

    private Long process(Long recordId, DebtPaperPushRequest request, String traceId,
                         boolean skipIdentityVerification) {
        DebtPaperPushPersistenceService.PersistedDebtPaper persisted;
        try {
            DebtPaperIssuanceRequest issuanceRequest = mapper.map(request);
            persisted = skipIdentityVerification
                    ? persistenceService.createImmediately(recordId, issuanceRequest)
                    : persistenceService.create(recordId, issuanceRequest);
        } catch (RuntimeException exception) {
            persistenceService.markNeedsReview(recordId, exception.getMessage());
            throw exception;
        }

        ExecutionPaper paper = persisted.executionPaper();
        if (paper == null) {
            if (persistenceService.status(recordId) == com.mparsict.vosolmaliat.domain.enums.DebtPaperProcessingStatus.REJECTED_IDENTITY)
                throw new BusinessException("IDENTITY_REJECTED", "درخواست صدور به علت عدم تأیید هویت رد شد");
            return null;
        }
        if (persisted.issuanceRequest().sourceDocument() != null && paper.getTaxUnitId() == null) {
            persistenceService.markWaitingOrganization(persisted.recordId());
            log.info("Sanim document accepted pending organization routing: executionPaperID={}", paper.getId());
            return paper.getId();
        }
        try {
            camundaProcessService.startPostIssuanceProcess(paper, persisted.issuanceRequest());
            persistenceService.markSuccessful(persisted.recordId());
            log.info(
                    "Debt Paper processed: referenceID={}, traceID={}, executionPaperID={}, result=SUCCESS",
                    request.referenceId(), traceId, paper.getId()
            );
            return paper.getId();
        } catch (RuntimeException exception) {
            persistenceService.markWorkflowFailed(persisted.recordId(), exception.getMessage());
            log.error(
                    "Debt Paper workflow failed: referenceID={}, traceID={}, executionPaperID={}, reason={}",
                    request.referenceId(), traceId, paper.getId(), exception.getClass().getSimpleName()
            );
            throw new DebtPaperPushException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    -99,
                    "Execution Paper was created, but workflow startup failed.",
                    paper.getId(),
                    exception
            );
        }
    }
}
