package com.mparsict.vosolmaliat.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mparsict.vosolmaliat.application.dto.request.DebtPaperIssuanceRequest;
import com.mparsict.vosolmaliat.application.dto.response.DashboardStatsResponse;
import com.mparsict.vosolmaliat.domain.enums.ExecutionPaperState;
import com.mparsict.vosolmaliat.domain.model.rule.ExecutionPaperIssuanceRequest;
import com.mparsict.vosolmaliat.domain.repository.DebtInformationRepository;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperIssuanceRequestRepository;
import com.mparsict.vosolmaliat.domain.repository.ExecutionPaperRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ExecutionPaperRepository executionPaperRepository;
    private final DebtInformationRepository debtInformationRepository;
    private final ExecutionPaperIssuanceRequestRepository issuanceRequestRepository;
    private final ObjectMapper objectMapper;
    private final com.mparsict.vosolmaliat.application.service.security.SecurityIdentityService identity;
    private final com.mparsict.vosolmaliat.application.service.security.ExecutionPaperVisibility visibility;
    private final com.mparsict.vosolmaliat.application.service.arrest.ArrestOrderService arrestOrders;
    private final com.mparsict.vosolmaliat.application.service.cartable.CartableOperations cartable;
    private final com.mparsict.vosolmaliat.domain.repository.security.AuditLogRepository audit;
    private final com.mparsict.vosolmaliat.domain.repository.ExecutionPaperNotificationRepository notifications;

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public DashboardStatsResponse getStats() {
        var user = identity.currentUser();
        var work = user.hasPermission("ARREST_ORDER_VIEW") ? arrestOrders.workStats()
                : new com.mparsict.vosolmaliat.application.service.arrest.ArrestOrderService.WorkStats(0, 0, 0, 0);
        long pendingTasks = 0;
        boolean taskListsAvailable = true;
        try {
            pendingTasks = cartable.myTasks(com.mparsict.vosolmaliat.application.dto.request.cartable.CartableTaskSearchCriteria.empty())
                    .stream().filter(task -> task.allowedActions() != null && (task.allowedActions().claim()
                            || task.allowedActions().complete() || task.allowedActions().assign() || task.allowedActions().delegate()))
                    .map(task -> task.taskKey()).distinct().count();
        } catch (RuntimeException ex) { taskListsAvailable = false; }
        long completedTasks = audit.countActedTasks(user.keycloakUserId());
        if (visibility.assignedOnly(user)) return assignedStats(work, pendingTasks, completedTasks, taskListsAvailable, visibility.paperIds(user));
        // Execution paper report counts
        long notInformedExecutionPapers = executionPaperRepository.countNotNotified();
        long informedExecutionPapers = executionPaperRepository.countNotified();
        long activeExecutionPapers = executionPaperRepository.countByCurrentStateNotIn(List.of(
                ExecutionPaperState.COMPLETED.name(),
                ExecutionPaperState.CANCELLED.name()
        ));
        long rejectedDebtPapers = issuanceRequestRepository.countByRejectionReasonIsNotNull();
        long totalIssuedExecutionPapers = executionPaperRepository.count();

        // Assignment report counts
        long unassigned = executionPaperRepository.countByCurrentState(ExecutionPaperState.UNASSIGNED.name());
        long arrestOrderIssued = executionPaperRepository.countByCurrentState(ExecutionPaperState.ARREST_ORDER_ISSUED.name());
        long arrestOrderNotIssued = executionPaperRepository.countByCurrentState(ExecutionPaperState.ARREST_ORDER_NOT_ISSUED.name());
        long awaitingArrestOrder = executionPaperRepository.countByCurrentState(ExecutionPaperState.AWAITING_ARREST_ORDER.name());

        // Debt totals aligned with the visible pie chart groups
        java.math.BigDecimal debtSumNotInformedExecutionPapers = debtInformationRepository.sumTotalDebtForNotInformedExecutionPapers();
        java.math.BigDecimal debtSumInformedExecutionPapers = debtInformationRepository.sumTotalDebtForInformedExecutionPapers();
        java.math.BigDecimal debtSumRejectedDebtPapers = sumRejectedDebtPapers();
        java.math.BigDecimal debtSumTotalIssuedExecutionPapers = debtInformationRepository.sumTotalDebtForIssuedExecutionPapers();

        // My actions
        long myActionsNew = 0;
        long myActionsAwaitingCompletion = work.pending() + pendingTasks;
        long myActionsCompleted = work.completed() + completedTasks;
        long myActionsCancelled = 0;

        // Financial totals
        java.math.BigDecimal totalDebtAwaitingSeizure = debtInformationRepository.sumOutstandingByState(ExecutionPaperState.UNASSIGNED.name());
        java.math.BigDecimal totalDebtWithSeizureOrder = debtInformationRepository.sumOutstandingByState(ExecutionPaperState.ARREST_ORDER_ISSUED.name());
        java.math.BigDecimal collectedAmounts = debtInformationRepository.sumPaidByState(ExecutionPaperState.COMPLETED.name());
        java.math.BigDecimal uncollectedAmounts = debtInformationRepository.sumOutstandingByState(ExecutionPaperState.CANCELLED.name());

        return new DashboardStatsResponse(
                notInformedExecutionPapers,
                informedExecutionPapers,
                activeExecutionPapers,
                rejectedDebtPapers,
                totalIssuedExecutionPapers,
                unassigned,
                arrestOrderIssued,
                arrestOrderNotIssued,
                awaitingArrestOrder,
                debtSumNotInformedExecutionPapers,
                debtSumInformedExecutionPapers,
                debtSumRejectedDebtPapers,
                debtSumTotalIssuedExecutionPapers,
                myActionsNew,
                myActionsAwaitingCompletion,
                myActionsCompleted,
                myActionsCancelled,
                totalDebtAwaitingSeizure,
                totalDebtWithSeizureOrder,
                collectedAmounts,
                uncollectedAmounts,
                executionPaperRepository.countByCurrentState(ExecutionPaperState.COMPLETED.name())
                    + executionPaperRepository.countByCurrentState(ExecutionPaperState.CANCELLED.name()),
                work, taskListsAvailable
        );
    }

    private DashboardStatsResponse assignedStats(
            com.mparsict.vosolmaliat.application.service.arrest.ArrestOrderService.WorkStats work,
            long pending, long completed, boolean available, List<Long> ids) {
        long notified = 0, active = 0, issued = 0, unassigned = 0, waiting = 0, closed = 0;
        var notInformedDebt = java.math.BigDecimal.ZERO;
        var informedDebt = java.math.BigDecimal.ZERO;
        var awaitingDebt = java.math.BigDecimal.ZERO;
        var issuedDebt = java.math.BigDecimal.ZERO;
        var collected = java.math.BigDecimal.ZERO;
        var uncollected = java.math.BigDecimal.ZERO;
        for (var paper : executionPaperRepository.findAllById(ids)) {
            String state = paper.getCurrentState();
            boolean informed = notifications.findByExecutionPaperId(paper.getId()).map(n -> n.getNotificationDate() != null).orElse(false);
            boolean isClosed = List.of("COMPLETED", "CANCELLED").contains(state);
            if (informed) notified++;
            if (isClosed) closed++; else active++;
            if ("ARREST_ORDER_ISSUED".equals(state)) issued++;
            if ("UNASSIGNED".equals(state)) unassigned++;
            if ("AWAITING_ARREST_ORDER".equals(state)) waiting++;
            var debt = debtInformationRepository.findByExecutionPaperId(paper.getId()).orElse(null);
            if (debt == null || debt.getExactOutstandingDebt() == null) continue;
            var amount = debt.getExactOutstandingDebt();
            if (informed) informedDebt = informedDebt.add(amount); else notInformedDebt = notInformedDebt.add(amount);
            if ("UNASSIGNED".equals(state)) awaitingDebt = awaitingDebt.add(amount);
            if ("ARREST_ORDER_ISSUED".equals(state)) issuedDebt = issuedDebt.add(amount);
            if ("COMPLETED".equals(state) && debt.getTotalPayments() != null) collected = collected.add(debt.getTotalPayments());
            if ("CANCELLED".equals(state)) uncollected = uncollected.add(amount);
        }
        return new DashboardStatsResponse(ids.size() - notified, notified, active, 0, ids.size(), unassigned,
                issued, ids.size() - issued, waiting, notInformedDebt, informedDebt, java.math.BigDecimal.ZERO,
                notInformedDebt.add(informedDebt), 0, work.pending() + pending, work.completed() + completed, 0,
                awaitingDebt, issuedDebt, collected, uncollected, closed, work, available);
    }

    private java.math.BigDecimal sumRejectedDebtPapers() {
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        for (var request : issuanceRequestRepository.findByRejectionReasonIsNotNull()) {
            try {
                var payload = objectMapper.readValue(request.getXml(), DebtPaperIssuanceRequest.class);
                if (payload.sourceDocument() == null || payload.sourceDocument().getAmounts() == null
                        || payload.sourceDocument().getAmounts().getOutstandingDebtAmount() == null) return null;
                total = total.add(payload.sourceDocument().getAmounts().getOutstandingDebtAmount());
            } catch (Exception ex) { return null; }
        }
        return total;
    }
}
