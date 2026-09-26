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
    private final FakeIdentityVerificationService identities;
    private final com.mparsict.vosolmaliat.common.util.UniqueCodeGenerator codes;
    private final com.mparsict.vosolmaliat.domain.repository.ExecutionPaperIssuanceRequestRepository requests;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long receive(DebtPaperPushRequest source, String traceId) {
        codes.lockIssuance();
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
                .sourceTaxOfficeCode(source.taxOfficeId())
                .announcementType(source.announcementType())
                .mappingVersion(SanimAggregateMapper.VERSION)
                .taxAmount(source.taxAmount())
                .taxSource(source.taxSource())
                .xmlPayload(source.xml())
                .build());

        return record.getId();
    }

    @Transactional
    public PersistedDebtPaper create(Long recordId, DebtPaperIssuanceRequest issuanceRequest) {
        return create(recordId, issuanceRequest, false);
    }

    @Transactional
    public PersistedDebtPaper createImmediately(Long recordId, DebtPaperIssuanceRequest issuanceRequest) {
        return create(recordId, issuanceRequest, true);
    }

    private PersistedDebtPaper create(Long recordId, DebtPaperIssuanceRequest issuanceRequest,
                                      boolean skipIdentityVerification) {
        codes.lockIssuance();
        DebtPaperPushRecord record = pushRecordRepository.findById(recordId).orElseThrow();
        if (record.getProcessingStatus() != DebtPaperProcessingStatus.RECEIVED
                && record.getProcessingStatus() != DebtPaperProcessingStatus.WAITING_IDENTITY)
            throw duplicate(record.getExecutionPaperId());
        SanimModelValidation.requireDocument(issuanceRequest.sourceDocument());
        String identityStatus = skipIdentityVerification ? "NOT_REQUIRED" : identities.status(issuanceRequest);
        if (record.getIssuanceRequestId() == null) {
            var request = issuanceRequestService.createAndSave(issuanceRequest);
            record.setIssuanceRequestId(request.getId());
        }
        requests.findById(record.getIssuanceRequestId()).ifPresent(r -> r.setIdentityStatus(identityStatus));
        if (!skipIdentityVerification && !"VERIFIED".equals(identityStatus)) {
            boolean rejected = "REJECTED".equals(identityStatus);
            record.setProcessingStatus(rejected ? DebtPaperProcessingStatus.REJECTED_IDENTITY : DebtPaperProcessingStatus.WAITING_IDENTITY);
            record.setFailureReason(rejected ? identities.rejectionReason(issuanceRequest.sourceDocument().getIdentityNumber()) : null);
            if (rejected) issuanceRequestService.markAsRejected(record.getIssuanceRequestId(),record.getFailureReason(),null);
            pushRecordRepository.save(record);
            return new PersistedDebtPaper(record.getId(),null,issuanceRequest);
        }
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

        ExecutionPaper paper = skipIdentityVerification
                ? executionPaperService.createFromRequest(issuanceRequest, true)
                : executionPaperService.createFromRequest(issuanceRequest);
        record.setExecutionPaper(paper);
        record.setSourceDocument(paper.getFinalizedDebtPaper());
        record.setProcessingStatus(DebtPaperProcessingStatus.PAPER_CREATED);
        pushRecordRepository.save(record);
        return new PersistedDebtPaper(record.getId(), paper, issuanceRequest);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markNeedsReview(Long recordId, String reason) {
        codes.lockIssuance();
        DebtPaperPushRecord record = pushRecordRepository.findById(recordId).orElseThrow();
        if (record.getProcessingStatus() != DebtPaperProcessingStatus.RECEIVED
                && record.getProcessingStatus() != DebtPaperProcessingStatus.WAITING_IDENTITY) return;
        record.setProcessingStatus(DebtPaperProcessingStatus.NEEDS_REVIEW);
        record.setFailureReason(reason);
        pushRecordRepository.save(record);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markWaitingOrganization(Long recordId) {
        codes.lockIssuance();
        DebtPaperPushRecord record = pushRecordRepository.findById(recordId).orElseThrow();
        record.setProcessingStatus(DebtPaperProcessingStatus.WAITING_ORGANIZATION);
        record.setFailureReason("Source document accepted; internal organization routing must be resolved before workflow startup.");
        pushRecordRepository.save(record);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccessful(Long recordId) {
        codes.lockIssuance();
        DebtPaperPushRecord record = pushRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalStateException("Debt Paper processing record was not found"));
        record.setProcessingStatus(DebtPaperProcessingStatus.SUCCESS);
        record.setFailureReason(null);
        pushRecordRepository.save(record);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markWorkflowFailed(Long recordId, String failureReason) {
        codes.lockIssuance();
        pushRecordRepository.findById(recordId).ifPresent(record -> {
            record.setProcessingStatus(DebtPaperProcessingStatus.WORKFLOW_START_FAILED);
            record.setFailureReason(failureReason);
            pushRecordRepository.save(record);
        });
    }

    @Transactional(readOnly = true)
    public DebtPaperPushRequest sourceRequest(Long recordId) {
        var r = pushRecordRepository.findById(recordId).orElseThrow();
        if (r.getProcessingStatus() != DebtPaperProcessingStatus.WAITING_IDENTITY)
            throw new com.mparsict.vosolmaliat.common.exception.BusinessException("NOT_WAITING_IDENTITY","درخواست در انتظار هویت نیست");
        return new DebtPaperPushRequest(r.getNationalId(),r.getXmlPayload(),r.getSourceHash(),r.getSourceProduceDate(),
            r.getReferenceId(),r.getFollowCode(),r.getSecondParameter(),r.getYear(),r.getPeriod(),r.getSourceTaxOfficeCode(),
            r.getTaxAmount(),r.getTaxSource(),r.getAnnouncementType());
    }
    public DebtPaperProcessingStatus status(Long recordId) {
        return pushRecordRepository.findById(recordId).orElseThrow().getProcessingStatus();
    }

    @Transactional(readOnly = true)
    Long waitingRecordId(String referenceId) {
        return pushRecordRepository.findByReferenceId(referenceId)
                .filter(record -> record.getProcessingStatus() == DebtPaperProcessingStatus.WAITING_IDENTITY
                        && record.getExecutionPaperId() == null)
                .map(DebtPaperPushRecord::getId)
                .orElse(null);
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
                "Debt Paper referenceID has already been processed.",
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
