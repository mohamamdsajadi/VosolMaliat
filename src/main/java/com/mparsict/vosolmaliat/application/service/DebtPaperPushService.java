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
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DebtPaperPushService {

    private final DebtPaperPushMapper mapper;
    private final DebtPaperPushPersistenceService persistenceService;
    private final CamundaProcessService camundaProcessService;

    public Long push(DebtPaperPushRequest request, String traceId) {
        log.info("Debt Paper processing started: refrenceID={}, traceID={}", request.referenceId(), traceId);
        persistenceService.rejectExistingReference(request.referenceId());
        DebtPaperIssuanceRequest issuanceRequest = mapper.map(request);

        DebtPaperPushPersistenceService.PersistedDebtPaper persisted;
        try {
            persisted = persistenceService.create(request, issuanceRequest, traceId);
        } catch (DataIntegrityViolationException exception) {
            log.warn("Concurrent duplicate Debt Paper rejected: refrenceID={}, traceID={}",
                    request.referenceId(), traceId);
            persistenceService.rejectExistingReference(request.referenceId());
            throw exception;
        }

        ExecutionPaper paper = persisted.executionPaper();
        try {
            camundaProcessService.startPostIssuanceProcess(paper, persisted.issuanceRequest());
            persistenceService.markSuccessful(persisted.recordId());
            log.info(
                    "Debt Paper processed: refrenceID={}, traceID={}, executionPaperID={}, result=SUCCESS",
                    request.referenceId(), traceId, paper.getId()
            );
            return paper.getId();
        } catch (BusinessException exception) {
            persistenceService.markWorkflowFailed(persisted.recordId(), exception.getMessage());
            log.error(
                    "Debt Paper workflow failed: refrenceID={}, traceID={}, executionPaperID={}, reason={}",
                    request.referenceId(), traceId, paper.getId(), exception.getErrorCode()
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
