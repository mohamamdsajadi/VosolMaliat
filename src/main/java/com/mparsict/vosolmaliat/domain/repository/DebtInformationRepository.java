package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.execution.DebtInformation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DebtInformationRepository extends JpaRepository<DebtInformation, Long> {

    @Query("SELECT d FROM DebtInformation d WHERE d.executionPaper.id IN :ids")
    java.util.List<DebtInformation> findByExecutionPaperIdIn(@Param("ids") java.util.Collection<Long> executionPaperIds);

    @Query("SELECT d FROM DebtInformation d WHERE d.executionPaper.id = :executionPaperId")
    Optional<DebtInformation> findByExecutionPaperId(@Param("executionPaperId") Long executionPaperId);

    @Query("SELECT d FROM DebtInformation d JOIN d.executionPaper ep WHERE ep.workflowState.currentState = :state")
    java.util.List<DebtInformation> findAmountsByState(@Param("state") String state);

    @Query("SELECT DISTINCT d FROM DebtInformation d LEFT JOIN ExecutionPaperNotification n ON n.executionPaper = d.executionPaper WHERE n.notificationDate IS NULL")
    java.util.List<DebtInformation> findNotInformedAmounts();

    @Query("SELECT DISTINCT d FROM DebtInformation d LEFT JOIN ExecutionPaperNotification n ON n.executionPaper = d.executionPaper WHERE n.notificationDate IS NOT NULL")
    java.util.List<DebtInformation> findInformedAmounts();

    default java.math.BigDecimal sumPaidByState(String state) {
        var rows = findAmountsByState(state);
        if (rows.stream().anyMatch(d -> d.getCurrentAmounts() == null || d.getCurrentAmounts().getTotalPaidAmount() == null)) return null;
        return rows.stream().map(d -> d.getCurrentAmounts().getTotalPaidAmount())
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }
    default java.math.BigDecimal sumOutstandingByState(String state) { return sum(findAmountsByState(state)); }
    default java.math.BigDecimal sumTotalDebtForNotInformedExecutionPapers() { return sum(findNotInformedAmounts()); }
    default java.math.BigDecimal sumTotalDebtForInformedExecutionPapers() { return sum(findInformedAmounts()); }
    default java.math.BigDecimal sumTotalDebtForIssuedExecutionPapers() { return sum(findAll()); }
    private static java.math.BigDecimal sum(java.util.List<DebtInformation> rows) {
        // Unknown legacy amounts are not reported as a known zero/partial total.
        if (rows.stream().anyMatch(d -> d.getExactOutstandingDebt() == null)) return null;
        return rows.stream().map(DebtInformation::getExactOutstandingDebt)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }
}
