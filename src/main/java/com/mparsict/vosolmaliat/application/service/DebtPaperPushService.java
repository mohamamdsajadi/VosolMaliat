package com.mparsict.vosolmaliat.application.service;

import com.mparsict.vosolmaliat.application.dto.request.DebtPaperIssuanceRequest;
import com.mparsict.vosolmaliat.application.dto.request.DebtPaperPushRequest;
import com.mparsict.vosolmaliat.application.dto.response.IssuanceResponse;
import com.mparsict.vosolmaliat.common.exception.DebtPaperPushException;
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
    private final RuleEngineService ruleEngineService;
    private final ExecutionPaperService executionPaperService;

    public IssuanceResponse push(DebtPaperPushRequest request, String traceId) {
        requireEf03(request);
        log.info("EF03 processing started: referenceID={}, traceID={}", request.referenceId(), traceId);

        Long recordId;
        try {
            recordId = persistenceService.receive(request, traceId);
        } catch (DataIntegrityViolationException exception) {
            persistenceService.rejectExistingReference(request.referenceId());
            throw exception;
        }

        DebtPaperIssuanceRequest issuanceRequest;
        try {
            issuanceRequest = mapper.map(request);
            persistenceService.prepareWorkflow(recordId, issuanceRequest);
        } catch (RuntimeException exception) {
            persistenceService.markNeedsReview(recordId, exception.getMessage());
            throw exception;
        }

        try {
            RuleEngineService.RuleEvaluationResult evaluation = ruleEngineService.evaluate(issuanceRequest);
            if (!evaluation.canIssue()) {
                IssuanceResponse result = new IssuanceResponse(
                        false, null, null, evaluation.rejectionReasonCode(),
                        evaluation.rejectionReasonDescription(), evaluation.restrictionRemovalDate(),
                        "صدور برگ اجرا توسط قوانین رد شد");
                persistenceService.markRejected(recordId,
                        rejectionMessage(result), result.restrictionRemovalDate());
                return result;
            }

            var paper = executionPaperService.createFromRequest(issuanceRequest);
            persistenceService.markSuccessful(recordId, paper.getId());
            return new IssuanceResponse(
                    true, paper.getId(), paper.getExecutionPaperUniqueCode(),
                    null, null, null, "برگ اجرا با موفقیت صادر شد");
        } catch (RuntimeException exception) {
            persistenceService.markWorkflowFailed(recordId, exception.getMessage());
            throw exception;
        }
    }

    private void requireEf03(DebtPaperPushRequest request) {
        String type = request.announcementType() == null ? "" : request.announcementType().trim();
        if (!"LTR_EF03".equalsIgnoreCase(type) && !"EF03".equalsIgnoreCase(type)) {
            throw new DebtPaperPushException(HttpStatus.UNPROCESSABLE_ENTITY, -7,
                    "Only LTR_EF03 debt papers are supported by this endpoint.");
        }
    }

    private String rejectionMessage(IssuanceResponse result) {
        String code = result.rejectionReasonCode() == null ? "RULE_REJECTED" : result.rejectionReasonCode();
        String description = result.rejectionReasonDescription() == null
                ? "Execution Paper issuance was rejected by the rule engine."
                : result.rejectionReasonDescription();
        return code + ": " + description;
    }
}
