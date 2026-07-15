package com.mparsict.vosolmaliat.application.service;

import com.mparsict.vosolmaliat.application.dto.response.DashboardStatsResponse;
import com.mparsict.vosolmaliat.domain.enums.ExecutionPaperState;
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
        Double totalDebtAwaitingSeizure = orZero(
                debtInformationRepository.sumRemainingTaxByState(ExecutionPaperState.UNASSIGNED.name()));
        Double totalDebtWithSeizureOrder = orZero(
                debtInformationRepository.sumRemainingTaxByState(ExecutionPaperState.ARREST_ORDER_ISSUED.name()));
        Double collectedAmounts = orZero(
                debtInformationRepository.sumRemainingTaxByState(ExecutionPaperState.COMPLETED.name()));
        Double uncollectedAmounts = orZero(
                debtInformationRepository.sumRemainingTaxByState(ExecutionPaperState.CANCELLED.name()));

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

    private Double orZero(Double val) {
        return val != null ? val : 0.0;
    }
}
