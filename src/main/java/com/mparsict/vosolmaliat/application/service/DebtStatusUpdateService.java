package com.mparsict.vosolmaliat.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.response.SanimDebtStatusResponse;
import com.mparsict.vosolmaliat.application.dto.response.SanimTaxpayerOptionResponse;
import com.mparsict.vosolmaliat.application.service.security.AuditLogService;
import com.mparsict.vosolmaliat.common.exception.BusinessException;
import com.mparsict.vosolmaliat.domain.model.execution.DebtInformation;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaper;
import com.mparsict.vosolmaliat.domain.model.inquiry.DebtInquiryResult;
import com.mparsict.vosolmaliat.domain.repository.DebtInformationRepository;
import com.mparsict.vosolmaliat.domain.repository.DebtInquiryResultRepository;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class DebtStatusUpdateService {

    private final ExecutionPaperRepository executionPaperRepository;
    private final DebtInformationRepository debtInformationRepository;
    private final DebtInquiryResultRepository debtInquiryResultRepository;
    private final SanimAuditService sanimAuditService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final com.mparsict.vosolmaliat.domain.repository.DebtInformationHistoryRepository historyRepository;

    @Transactional
    public void update(Long executionPaperId, String requestId, SanimDebtStatusResponse status) {
        ExecutionPaper executionPaper = executionPaperRepository.findById(executionPaperId)
                .orElseThrow(() -> new BusinessException("EXECUTION_PAPER_NOT_FOUND", "برگ اجرا یافت نشد"));
        validateTaxpayer(executionPaper, status);

        String responseJson = serialize(status);
        String responseHash = sha256(responseJson);
        var existing = debtInquiryResultRepository.findByRequestId(requestId);
        if (existing.isPresent()) {
            validateExistingResult(existing.get(), executionPaperId, responseHash);
            return; // identical delivery cannot revert a newer result
        }
        var patch = status.amounts() != null ? status.amounts().copy()
                : com.mparsict.vosolmaliat.domain.model.sanim.SanimAmounts.builder()
                    .outstandingDebtAmount(java.math.BigDecimal.valueOf(status.outstandingDebtRials()))
                    .totalPenaltyAmount(java.math.BigDecimal.valueOf(status.totalPenaltyRials()))
                    .totalPaidAmount(java.math.BigDecimal.valueOf(status.totalPaymentRials()))
                    .totalCreditAmount(java.math.BigDecimal.valueOf(status.totalCreditRials())).build();
        var effectiveAt = status.lastActivityAt();
        debtInquiryResultRepository.save(DebtInquiryResult.builder()
                .requestId(requestId).finalizedDebtPaperNotificationId(executionPaper.getFinalizedDebtPaper() == null ? null : executionPaper.getFinalizedDebtPaper().getFinalTaxDebtNotificationNumber()).executionPaper(executionPaper).responseJson(responseJson).hash(responseHash)
                .receivedAt(LocalDateTime.now()).effectiveAt(effectiveAt).amounts(patch.copy())
                .mappingVersion("sanim-inquiry-v1").build());

        DebtInformation debt = debtInformationRepository.findByExecutionPaperId(executionPaperId)
                .orElseGet(() -> DebtInformation.builder().executionPaper(executionPaper).finalizedDebtPaper(executionPaper.getFinalizedDebtPaper()).build());
        if (debt.getCurrentAmountsAsOf() != null
                && (effectiveAt == null || !effectiveAt.isAfter(debt.getCurrentAmountsAsOf()))) return;
        // Without an effective timestamp a Sanim document cannot safely be overwritten.
        if (effectiveAt == null && debt.getCurrentAmounts() != null) return;
        if (debt.getId() != null && debt.getCurrentAmounts() != null) {
            historyRepository.findFirstByDebtInformationIdAndNextInquiryDateIsNullOrderByIdDesc(debt.getId())
                    .ifPresent(previous -> { previous.close(LocalDateTime.now()); historyRepository.save(previous); });
            // Inquiry responses carry aggregates only; old breakdowns remain in the source snapshot/history.
            if (debt.getFinancialDetails() != null) {
                var details=debt.getFinancialDetails(); var old=debt.getCurrentAmounts();
                if (changed(old.getTotalTaxAmount(),patch.getTotalTaxAmount())) details.setTaxLiabilityDetails(null);
                if (changed(old.getTotalCreditAmount(),patch.getTotalCreditAmount())) details.setTaxCreditDetails(null);
                if (changed(old.getTotalPenaltyAmount(),patch.getTotalPenaltyAmount())) details.setTotalPenaltiesDetails(null);
                if (changed(old.getTotalPaidAmount(),patch.getTotalPaidAmount())) details.setTotalPaymentsDetails(null);
                if (changed(old.getTotalRemissionAmount(),patch.getTotalRemissionAmount())) details.setTotalRemissionsDetails(null);
                if (changed(old.getOutstandingDebtAmount(),patch.getOutstandingDebtAmount())) details.setOutstandingDebtDetails(null);
            }
        }
        if (debt.getCurrentAmounts() == null)
            debt.setCurrentAmounts(new com.mparsict.vosolmaliat.domain.model.sanim.SanimAmounts());
        debt.getCurrentAmounts().merge(patch);
        debt.setCurrentAmountsAsOf(effectiveAt);
        debt.setLastDebtInquiryAt(LocalDateTime.now());
        if (debt.getFinalizedDebtPaper() == null) debt.setFinalizedDebtPaper(executionPaper.getFinalizedDebtPaper());
        SanimModelValidation.requireAmounts(debt.getCurrentAmounts());
        debtInformationRepository.save(debt);
        historyRepository.save(com.mparsict.vosolmaliat.domain.model.execution.DebtInformationHistory.current(
                debt, requestId, debt.getLastDebtInquiryAt()));

        auditLogService.record(
                null,
                "TAXPAYER_DEBT_STATUS_UPDATED_FROM_SANIM",
                "EXECUTION_PAPER",
                executionPaperId.toString(),
                null,
                requestId + ":" + debt.getCurrentAmounts().getOutstandingDebtAmount()
        );
    }

    private boolean changed(java.math.BigDecimal old, java.math.BigDecimal next) { return next != null && (old == null || old.compareTo(next) != 0); }

    private void validateTaxpayer(ExecutionPaper executionPaper, SanimDebtStatusResponse status) {
        if (executionPaper.getTaxpayerId() == null) {
            throw new BusinessException("EXECUTION_PAPER_TAXPAYER_MISSING", "مؤدی برگ اجرا مشخص نشده است");
        }
        SanimTaxpayerOptionResponse taxpayer = sanimAuditService.taxpayer(executionPaper.getTaxpayerId());
        if (taxpayer.taxpayerType() != status.taxpayerType()
                || !taxpayer.identifier().equals(status.taxpayerIdentifier())) {
            throw new BusinessException(
                    "SANIM_DEBT_TAXPAYER_MISMATCH",
                    "پاسخ بدهی سنیم متعلق به مؤدی برگ اجرا نیست"
            );
        }
    }

    private void validateExistingResult(
            DebtInquiryResult existing,
            Long executionPaperId,
            String responseHash) {
        if (!executionPaperId.equals(existing.getExecutionPaper().getId())
                || !responseHash.equals(existing.getHash())) {
            throw new BusinessException(
                    "SANIM_DEBT_INQUIRY_RESULT_CONFLICT",
                    "پاسخ دیگری قبلاً برای شناسه همبستگی استعلام بدهی ثبت شده است"
            );
        }
    }

    private String serialize(SanimDebtStatusResponse status) {
        try {
            return objectMapper.writeValueAsString(status);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("SANIM_DEBT_RESPONSE_SERIALIZATION_FAILED", "خطا در ذخیره پاسخ سنیم", ex);
        }
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
