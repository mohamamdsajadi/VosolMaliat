package com.mparsict.vosolmaliat.application.service;

import com.mparsict.vosolmaliat.application.service.security.AuditLogService;
import com.mparsict.vosolmaliat.common.exception.BusinessException;
import com.mparsict.vosolmaliat.domain.enums.ExecutionPaperState;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaper;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaperNotification;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaperStateHistory;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperNotificationRepository;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperRepository;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperStateHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InformProcessService {

    private static final ZoneId TEHRAN = ZoneId.of("Asia/Tehran");

    private final InformRuleEngineService informRuleEngineService;
    private final FakeElectronicNotificationService fakeElectronicNotificationService;
    private final FakeSanimInformService fakeSanimInformService;
    private final ExecutionPaperRepository executionPaperRepository;
    private final ExecutionPaperNotificationRepository notificationRepository;
    private final ExecutionPaperStateHistoryRepository stateHistoryRepository;
    private final AuditLogService auditLogService;

    public Map<String, Object> checkInformPossibility(Long executionPaperId) {
        InformRuleEngineService.InformRuleEvaluationResult result = informRuleEngineService.evaluate(executionPaperId);
        Map<String, Object> variables = new HashMap<>();
        variables.put("canInform", result.canInform());
        if (!result.canInform()) {
            variables.put("rejectionReasonCode", result.rejectionReasonCode());
            variables.put("rejectionReasonDesc", result.rejectionReasonDescription());
            variables.put("restrictionRemovalDate", result.restrictionRemovalDate());
        }
        return variables;
    }

    @Transactional
    public void markSentToElectronicNotification(Long executionPaperId) {
        ExecutionPaper executionPaper = findExecutionPaper(executionPaperId);
        ExecutionPaperNotification notification = findOrCreateNotification(executionPaper);
        notification.setInformStatus(ExecutionPaperState.INFORM_SENT.name());
        notificationRepository.save(notification);
        changeState(executionPaper, ExecutionPaperState.INFORM_SENT.name());
        fakeElectronicNotificationService.send(executionPaperId);
    }

    @Transactional
    public void updateInformStatus(Long executionPaperId, Boolean canInform, String informDate, String rejectionCode, String rejectionDesc) {
        ExecutionPaper executionPaper = findExecutionPaper(executionPaperId);
        ExecutionPaperNotification notification = findOrCreateNotification(executionPaper);
        if (Boolean.FALSE.equals(canInform)) {
            String reason = joinReason(rejectionCode, rejectionDesc);
            notification.setInformStatus(ExecutionPaperState.INFORM_REJECTED.name());
            notification.setInformRejectionReason(reason);
            notification.setInformRejectedAt(LocalDateTime.now().toString());
            notificationRepository.save(notification);
            changeState(executionPaper, ExecutionPaperState.INFORM_REJECTED.name());
            auditLogService.record(null, "EXECUTION_PAPER_INFORM_REJECTED", "EXECUTION_PAPER",
                    executionPaperId.toString(), null, reason);
            return;
        }

        notification.setInformStatus(ExecutionPaperState.INFORMED.name());
        notification.setElectronicNotificationDate(informDate);
        notificationRepository.save(notification);
        changeState(executionPaper, ExecutionPaperState.INFORMED.name());
        auditLogService.record(null, "EXECUTION_PAPER_INFORMED", "EXECUTION_PAPER",
                executionPaperId.toString(), null, informDate);
    }

    @Transactional
    public String setReInformDate(Long executionPaperId, String restrictionRemovalDate) {
        ExecutionPaper executionPaper = findExecutionPaper(executionPaperId);
        LocalDate baseDate = parseDateOrToday(restrictionRemovalDate);
        String reInformDate = OffsetDateTime.of(baseDate.plusDays(1).atStartOfDay(), TEHRAN.getRules().getOffset(LocalDateTime.now()))
                .toString();
        ExecutionPaperNotification notification = findOrCreateNotification(executionPaper);
        notification.setReInformDate(reInformDate);
        notificationRepository.save(notification);
        auditLogService.record(null, "EXECUTION_PAPER_RE_INFORM_SCHEDULED", "EXECUTION_PAPER",
                executionPaperId.toString(), null, reInformDate);
        return reInformDate;
    }

    @Transactional
    public String setNextDebtInquiryDate(Long executionPaperId, String informDate) {
        ExecutionPaper executionPaper = findExecutionPaper(executionPaperId);
        String nextDate = parseDateOrToday(informDate).plusMonths(1).toString();
        ExecutionPaperNotification notification = findOrCreateNotification(executionPaper);
        notification.setNextDebtInquiryDate(nextDate);
        notificationRepository.save(notification);
        auditLogService.record(null, "NEXT_DEBT_INQUIRY_DATE_SET", "EXECUTION_PAPER",
                executionPaperId.toString(), informDate, nextDate);
        return nextDate;
    }

    public void sendInformDateToSanim(Long executionPaperId, String informDate) {
        fakeSanimInformService.sendInformDate(executionPaperId, informDate);
    }

    private ExecutionPaper findExecutionPaper(Long executionPaperId) {
        return executionPaperRepository.findById(executionPaperId)
                .orElseThrow(() -> new BusinessException("EXECUTION_PAPER_NOT_FOUND", "برگ اجرا یافت نشد"));
    }

    private ExecutionPaperNotification findOrCreateNotification(ExecutionPaper executionPaper) {
        return notificationRepository.findByExecutionPaperId(executionPaper.getId())
                .orElseGet(() -> ExecutionPaperNotification.builder().executionPaper(executionPaper).build());
    }

    private void changeState(ExecutionPaper executionPaper, String state) {
        executionPaper.setCurrentState(state);
        executionPaperRepository.save(executionPaper);
        stateHistoryRepository.save(ExecutionPaperStateHistory.builder()
                .executionPaper(executionPaper)
                .state(state)
                .stateDate(LocalDate.now().toString())
                .build());
    }

    private String joinReason(String rejectionCode, String rejectionDesc) {
        if (rejectionCode == null || rejectionCode.isBlank()) {
            return rejectionDesc;
        }
        if (rejectionDesc == null || rejectionDesc.isBlank()) {
            return rejectionCode;
        }
        return rejectionCode + ": " + rejectionDesc;
    }

    private LocalDate parseDateOrToday(String value) {
        if (value == null || value.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(value.length() >= 10 ? value.substring(0, 10) : value);
        } catch (Exception ex) {
            return LocalDate.now();
        }
    }
}
