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

    public DashboardStatsResponse getStats() {
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
        long myActionsNew = unassigned;
        long myActionsAwaitingCompletion =
                executionPaperRepository.countByCurrentState(ExecutionPaperState.ASSIGNED.name())
                + executionPaperRepository.countByCurrentState(ExecutionPaperState.NOTIFIED.name())
                + executionPaperRepository.countByCurrentState(ExecutionPaperState.NOTIFICATION_FAILED.name())
                + awaitingArrestOrder;
        long myActionsCompleted = executionPaperRepository.countByCurrentState(ExecutionPaperState.COMPLETED.name());
        long myActionsCancelled = executionPaperRepository.countByCurrentState(ExecutionPaperState.CANCELLED.name());

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
                uncollectedAmounts
        );
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
