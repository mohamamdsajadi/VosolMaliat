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
import com.mparsict.vosolmaliat.infrastructure.camunda.ProcessConstants;
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
        variables.put(ProcessConstants.VAR_CAN_INFORM, result.canInform());
        if (!result.canInform()) {
            variables.put(ProcessConstants.VAR_REJECTION_CODE, result.rejectionReasonCode());
            variables.put(ProcessConstants.VAR_REJECTION_DESC, result.rejectionReasonDescription());
            variables.put(ProcessConstants.VAR_RESTRICTION_REMOVAL_DATE, result.restrictionRemovalDate());
        }
        return variables;
    }

    @Transactional
    public void markSentToElectronicNotification(Long executionPaperId) {
        ExecutionPaper executionPaper = findExecutionPaper(executionPaperId);
        changeState(executionPaper, ExecutionPaperState.INFORM_SENT.name());
        var sent = fakeElectronicNotificationService.send(executionPaperId);
        executionPaper.getWorkflowState().setInformStatus(ExecutionPaperState.INFORM_SENT.name());
        executionPaper.getWorkflowState().setNotificationTrackingId(sent.externalTrackingCode());
    }

    @Transactional
    public void updateInformStatus(Long executionPaperId, Boolean canInform, String informDate, String rejectionCode, String rejectionDesc) {
        ExecutionPaper executionPaper = findExecutionPaper(executionPaperId);
        var workflow = workflow(executionPaper);
        if (Boolean.FALSE.equals(canInform)) {
            String reason = joinReason(rejectionCode, rejectionDesc);
            workflow.setInformStatus(ExecutionPaperState.INFORM_REJECTED.name());
            workflow.setInformRejectionReason(reason);
            workflow.setInformRejectedAt(LocalDateTime.now().toString());
            executionPaperRepository.save(executionPaper);
            changeState(executionPaper, ExecutionPaperState.INFORM_REJECTED.name());
            auditLogService.record(null, "EXECUTION_PAPER_INFORM_REJECTED", "EXECUTION_PAPER",
                    executionPaperId.toString(), null, reason);
            return;
        }

        String normalizedInformDate = normalizeInformDateTime(informDate);
        ExecutionPaperNotification notification = notificationRepository.findByExecutionPaperId(executionPaperId)
                .orElseGet(() -> ExecutionPaperNotification.builder().executionPaper(executionPaper)
                        .electronicNotificationId(workflow.getNotificationTrackingId()).build());
        if (notification.getElectronicNotificationId() == null)
            throw new BusinessException("NOTIFICATION_ID_REQUIRED", "شناسه ابلاغ الکترونیک دریافت نشده است");
        workflow.setInformStatus(ExecutionPaperState.INFORMED.name());
        notification.setElectronicNotificationDate(normalizedInformDate);
        notificationRepository.save(notification);
        changeState(executionPaper, ExecutionPaperState.INFORMED.name());
        auditLogService.record(null, "EXECUTION_PAPER_INFORMED", "EXECUTION_PAPER",
                executionPaperId.toString(), null, normalizedInformDate);
    }

    @Transactional
    public String setReInformDate(Long executionPaperId, String restrictionRemovalDate) {
        ExecutionPaper executionPaper = findExecutionPaper(executionPaperId);
        LocalDate baseDate = parseDateOrToday(restrictionRemovalDate);
        String reInformDate = OffsetDateTime.of(baseDate.plusDays(1).atStartOfDay(), TEHRAN.getRules().getOffset(LocalDateTime.now()))
                .toString();
        workflow(executionPaper).setReInformDate(reInformDate);
        executionPaperRepository.save(executionPaper);
        auditLogService.record(null, "EXECUTION_PAPER_RE_INFORM_SCHEDULED", "EXECUTION_PAPER",
                executionPaperId.toString(), null, reInformDate);
        return reInformDate;
    }

    @Transactional
    public String setNextDebtInquiryDate(Long executionPaperId, String informDate) {
        ExecutionPaper executionPaper = findExecutionPaper(executionPaperId);
        var notification = notificationRepository.findByExecutionPaperId(executionPaperId)
                .orElseThrow(() -> new BusinessException("NOTIFICATION_NOT_FOUND", "ابلاغ برگ اجرا یافت نشد"));
        if (notification.paymentDeadline() == null)
            throw new BusinessException("NOTIFICATION_NOT_VIEWED", "مهلت سی‌روزه پس از ثبت تاریخ رویت محاسبه می‌شود");
        String nextDate = notification.paymentDeadline().toString();
        workflow(executionPaper).setNextDebtInquiryDate(nextDate);
        executionPaperRepository.save(executionPaper);
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

    private com.mparsict.vosolmaliat.domain.model.execution.ExecutionWorkflowState workflow(ExecutionPaper paper) {
        if (paper.getWorkflowState() == null) paper.setCurrentState("UNASSIGNED");
        return paper.getWorkflowState();
    }

    @Transactional
    public void recordOrCorrectView(Long executionPaperId, String persianDate, String reason) {
        var notification = notificationRepository.findByExecutionPaperId(executionPaperId)
                .orElseThrow(() -> new BusinessException("NOTIFICATION_NOT_FOUND", "ابلاغ برگ اجرا یافت نشد"));
        LocalDateTime date;
        try { date = com.mparsict.vosolmaliat.domain.value.PersianDates.parse(persianDate); }
        catch (IllegalArgumentException ex) { throw new BusinessException("INVALID_VIEW_DATE", ex.getMessage()); }
        var previous = notification.getTaxpayerNotificationViewDate();
        if (previous != null && (reason == null || reason.isBlank()))
            throw new BusinessException("CORRECTION_REASON_REQUIRED", "علت اصلاح تاریخ رویت الزامی است");
        if (java.util.Objects.equals(previous, date)) return;
        notification.recordView(date);
        notificationRepository.save(notification);
        workflow(notification.getExecutionPaper()).setNextDebtInquiryDate(notification.paymentDeadline().toString());
        workflow(notification.getExecutionPaper()).setViewDateMessagePending(true);
        auditLogService.record(null, "NOTIFICATION_VIEW_DATE_UPDATED", "EXECUTION_PAPER", executionPaperId.toString(),
                previous == null ? null : previous.toString(), date + (reason == null ? "" : " / " + reason));
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

    private String normalizeInformDateTime(String informDate) {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        if (informDate == null || informDate.isBlank()) {
            return now.toString();
        }

        String raw = informDate.trim();
        String normalized = raw.contains("T") ? raw : raw.replace(" ", "T");
        try {
            return OffsetDateTime.parse(normalized)
                    .atZoneSameInstant(TEHRAN)
                    .toLocalDateTime()
                    .withSecond(0)
                    .withNano(0)
                    .toString();
        } catch (Exception ignored) {
            // Fall through to local date-time or date-only parsing.
        }
        try {
            return LocalDateTime.parse(normalized)
                    .withSecond(0)
                    .withNano(0)
                    .toString();
        } catch (Exception ignored) {
            // Fall through to legacy date-only parsing.
        }
        try {
            return LocalDate.parse(raw.substring(0, Math.min(raw.length(), 10))).atTime(now.toLocalTime()).toString();
        } catch (Exception ignored) {
            return now.toString();
        }
    }
}
