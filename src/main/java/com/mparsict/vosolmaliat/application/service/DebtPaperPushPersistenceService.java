package com.mparsict.vosolmaliat.application.service;

import com.mparsict.vosolmaliat.application.dto.request.DebtPaperIssuanceRequest;
import com.mparsict.vosolmaliat.application.dto.request.DebtPaperPushRequest;
import com.mparsict.vosolmaliat.common.exception.DebtPaperPushException;
import com.mparsict.vosolmaliat.domain.enums.DebtPaperProcessingStatus;
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
    private final com.mparsict.vosolmaliat.domain.repository.TaxpayerRepository taxpayers;
    private final com.mparsict.vosolmaliat.domain.repository.FinalTaxDebtPaperRepository sourcePapers;
    private final com.mparsict.vosolmaliat.common.util.UniqueCodeGenerator codes;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long receive(DebtPaperPushRequest source, String traceId) {
        codes.lockIssuance();
        var existing = pushRecordRepository.findByReferenceId(source.referenceId());
        if (existing.isPresent()) {
            DebtPaperPushRecord record = existing.get();
            // A result timeout does not prove Camunda failed to start. Reserve this reference.
            throw duplicate(record.getExecutionPaperId());
        }
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
                .sourceTaxOfficeCode(source.taxOfficeId())
                .announcementType(source.announcementType())
                .mappingVersion(SanimAggregateMapper.VERSION)
                .taxAmount(source.taxAmount())
                .taxSource(source.taxSource())
                .xmlPayload(source.xml())
                .build());
        return record.getId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long prepareWorkflow(Long recordId, DebtPaperIssuanceRequest request) {
        codes.lockIssuance();
        TaxpayerIdentityValidation.requireConsistentRegistration(taxpayers, request);
        sourcePapers.findByUniqueFinalTaxDebtNoticeId(request.debtPaperUniqueCode()).ifPresent(source -> {
            if (!java.util.Objects.equals(source.getTaxpayer().getTin(), request.sourceDocument().getTaxpayerIdentifier())) {
                throw new com.mparsict.vosolmaliat.common.exception.BusinessException(
                        "DEBT_PAPER_TAXPAYER_CONFLICT", "برگ بدهی قطعی به مؤدی دیگری تعلق دارد");
            }
        });
        DebtPaperPushRecord record = pushRecordRepository.findById(recordId).orElseThrow();
        if (record.getProcessingStatus() != DebtPaperProcessingStatus.RECEIVED) {
            throw duplicate(record.getExecutionPaperId());
        }
        if (record.getIssuanceRequestId() == null) {
            var saved = issuanceRequestService.createAndSave(request);
            record.setIssuanceRequestId(saved.getId());
        }
        record.setProcessingStatus(DebtPaperProcessingStatus.PROCESSING);
        pushRecordRepository.save(record);
        return record.getIssuanceRequestId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long resumeSampleImport(Long recordId) {
        codes.lockIssuance();
        DebtPaperPushRecord record = pushRecordRepository.findById(recordId).orElseThrow();
        if (record.getExecutionPaperId() != null
                || record.getTraceId() == null
                || !record.getTraceId().startsWith("ef03-sample-import-")) {
            throw duplicate(record.getExecutionPaperId());
        }
        record.setProcessingStatus(DebtPaperProcessingStatus.RECEIVED);
        record.setFailureReason(null);
        pushRecordRepository.saveAndFlush(record);
        return record.getId();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccessful(Long recordId, Long executionPaperId) {
        codes.lockIssuance();
        DebtPaperPushRecord record = pushRecordRepository.findById(recordId).orElseThrow();
        var paper = executionPaperRepository.findById(executionPaperId).orElseThrow();
        record.setExecutionPaper(paper);
        record.setFinalTaxDebtPaper(paper.getFinalTaxDebtPaper());
        record.setProcessingStatus(DebtPaperProcessingStatus.SUCCESS);
        record.setFailureReason(null);
        pushRecordRepository.save(record);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markRejected(Long recordId, String reason, String retryDate) {
        codes.lockIssuance();
        DebtPaperPushRecord record = pushRecordRepository.findById(recordId).orElseThrow();
        record.setProcessingStatus(DebtPaperProcessingStatus.REJECTED_BY_RULE);
        record.setFailureReason(retryDate == null || retryDate.isBlank()
                ? reason : reason + " (next retry date: " + retryDate + ")");
        pushRecordRepository.save(record);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markNeedsReview(Long recordId, String reason) {
        codes.lockIssuance();
        pushRecordRepository.findById(recordId).ifPresent(record -> {
            record.setProcessingStatus(DebtPaperProcessingStatus.NEEDS_REVIEW);
            record.setFailureReason(reason);
            pushRecordRepository.save(record);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markWorkflowFailed(Long recordId, String failureReason) {
        codes.lockIssuance();
        pushRecordRepository.findById(recordId).ifPresent(record -> {
            if (record.getExecutionPaperId() != null
                    || record.getProcessingStatus() == DebtPaperProcessingStatus.REJECTED_BY_RULE) return;
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
        return new DebtPaperPushException(HttpStatus.CONFLICT, -8,
                "Debt Paper referenceID has already been processed.",
                executionPaperId, null);
    }
}
