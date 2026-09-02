package com.mparsict.vosolmaliat.application.service;

import com.mparsict.vosolmaliat.application.dto.request.DebtPaperIssuanceRequest;
import com.mparsict.vosolmaliat.application.dto.request.DebtPaperPushRequest;
import com.mparsict.vosolmaliat.common.exception.DebtPaperPushException;
import com.mparsict.vosolmaliat.domain.enums.DebtPaperProcessingStatus;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaper;
import com.mparsict.vosolmaliat.domain.model.integration.DebtPaperPushRecord;
import com.mparsict.vosolmaliat.domain.repository.DebtPaperPushRecordRepository;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DebtPaperPushPersistenceService {

    private final DebtPaperPushRecordRepository pushRecordRepository;
    private final ExecutionPaperRepository executionPaperRepository;
    private final IssuanceRequestService issuanceRequestService;
    private final RuleEngineService ruleEngineService;
    private final ExecutionPaperService executionPaperService;

    @Transactional
    public PersistedDebtPaper create(
            DebtPaperPushRequest source,
            DebtPaperIssuanceRequest issuanceRequest,
            String traceId) {
        rejectExistingReference(source.referenceId());
        executionPaperRepository
                .findByFinalizedDebtPaperDebtPaperUniqueCode(source.referenceId())
                .ifPresent(paper -> {
                    throw duplicate(paper.getId());
                });

        DebtPaperPushRecord record = pushRecordRepository.saveAndFlush(DebtPaperPushRecord.builder()
                .referenceId(source.referenceId())
                .traceId(traceId)
                .processingStatus(DebtPaperProcessingStatus.RECEIVED)
                .receivedAt(LocalDateTime.now())
                .sourceProduceDate(source.produceDate())
                .sourceHash(source.hash())
                .nationalId(source.nationalID())
                .followCode(source.followCode())
                .secondParameter(source.secondParameter())
                .year(source.year())
                .period(source.period())
                .taxOfficeId(source.taxOfficeId())
                .taxAmount(source.taxAmount())
                .taxSource(source.taxSource())
                .xmlPayload(source.xml())
                .build());

        RuleEngineService.RuleEvaluationResult evaluation = ruleEngineService.evaluate(issuanceRequest);
        if (!evaluation.canIssue()) {
            throw new DebtPaperPushException(
                    HttpStatus.BAD_REQUEST,
                    -7,
                    evaluation.rejectionReasonDescription() == null
                            ? "Execution Paper issuance is not permitted."
                            : evaluation.rejectionReasonDescription()
            );
        }

        issuanceRequestService.createAndSave(issuanceRequest);
        ExecutionPaper paper = executionPaperService.createFromRequest(issuanceRequest);
        record.setExecutionPaper(paper);
        record.setProcessingStatus(DebtPaperProcessingStatus.PAPER_CREATED);
        pushRecordRepository.save(record);
        return new PersistedDebtPaper(record.getId(), paper, issuanceRequest);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccessful(Long recordId) {
        DebtPaperPushRecord record = pushRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalStateException("Debt Paper processing record was not found"));
        record.setProcessingStatus(DebtPaperProcessingStatus.SUCCESS);
        record.setFailureReason(null);
        pushRecordRepository.save(record);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markWorkflowFailed(Long recordId, String failureReason) {
        pushRecordRepository.findById(recordId).ifPresent(record -> {
            record.setProcessingStatus(DebtPaperProcessingStatus.WORKFLOW_START_FAILED);
            record.setFailureReason(failureReason);
            pushRecordRepository.save(record);
        });
    }

    public void rejectExistingReference(String referenceId) {
        pushRecordRepository.findByReferenceId(referenceId).ifPresent(record -> {
            throw duplicate(record.getExecutionPaperId());
        });
    }

    private DebtPaperPushException duplicate(Long executionPaperId) {
        return new DebtPaperPushException(
                HttpStatus.CONFLICT,
                -8,
                "Debt Paper refrenceID has already been processed.",
                executionPaperId,
                null
        );
    }

    public record PersistedDebtPaper(
            Long recordId,
            ExecutionPaper executionPaper,
            DebtPaperIssuanceRequest issuanceRequest
    ) {
    }
}
