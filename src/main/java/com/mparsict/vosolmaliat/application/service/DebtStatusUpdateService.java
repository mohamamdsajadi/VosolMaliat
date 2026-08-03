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

    @Transactional
    public void update(Long executionPaperId, String requestId, SanimDebtStatusResponse status) {
        ExecutionPaper executionPaper = executionPaperRepository.findById(executionPaperId)
                .orElseThrow(() -> new BusinessException("EXECUTION_PAPER_NOT_FOUND", "برگ اجرا یافت نشد"));
        validateTaxpayer(executionPaper, status);

        String responseJson = serialize(status);
        String responseHash = sha256(responseJson);
        debtInquiryResultRepository.findByRequestId(requestId).ifPresentOrElse(
                existing -> validateExistingResult(existing, executionPaperId, responseHash),
                () -> debtInquiryResultRepository.save(DebtInquiryResult.builder()
                        .requestId(requestId)
                        .executionPaper(executionPaper)
                        .responseJson(responseJson)
                        .hash(responseHash)
                        .receivedAt(LocalDateTime.now())
                        .build())
        );

        DebtInformation debt = debtInformationRepository.findByExecutionPaperId(executionPaperId)
                .orElseGet(() -> DebtInformation.builder().executionPaper(executionPaper).build());
        debt.setSanimTotalDebtRials(status.totalDebtRials());
        debt.setSanimTotalPenaltyRials(status.totalPenaltyRials());
        debt.setSanimTotalPaymentRials(status.totalPaymentRials());
        debt.setSanimTotalCreditRials(status.totalCreditRials());
        debt.setSanimOutstandingDebtRials(status.outstandingDebtRials());
        debt.setSanimLastActivityAt(status.lastActivityAt());
        debt.setLastDebtInquiryAt(LocalDateTime.now());
        debtInformationRepository.save(debt);

        auditLogService.record(
                null,
                "TAXPAYER_DEBT_STATUS_UPDATED_FROM_SANIM",
                "EXECUTION_PAPER",
                executionPaperId.toString(),
                null,
                requestId + ":" + status.outstandingDebtRials()
        );
    }

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
