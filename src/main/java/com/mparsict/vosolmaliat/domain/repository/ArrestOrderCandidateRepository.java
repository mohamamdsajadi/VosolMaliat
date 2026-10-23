package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

public interface ArrestOrderCandidateRepository extends Repository<ExecutionPaper, Long> {
    @Query("""
        SELECT ep FROM ExecutionPaper ep
        JOIN ep.finalTaxDebtPaper dp JOIN dp.taxpayer tp
        LEFT JOIN tp.naturalPerson np LEFT JOIN np.extraInfo ni
        LEFT JOIN tp.legalPerson lp LEFT JOIN lp.extraInfo li
        JOIN ExecutionPaperNotification n ON n.executionPaper = ep
        JOIN DebtInformation debt ON debt.finalTaxDebtPaper = dp
        WHERE (:national = true OR dp.taxAffairsOffice.generalDirectorateOfTaxAffairs.id = :directorateId)
          AND ep.workflowState.currentState NOT IN ('COMPLETED', 'CANCELLED', 'ARREST_ORDER_ISSUED')
          AND n.notificationDate IS NOT NULL AND n.taxpayerNotificationViewDate <= :deadlineCutoff
          AND cast(debt.outstandingTaxDebt as BigDecimal) > 0
          AND NOT EXISTS (SELECT ao.id FROM ArrestOrder ao WHERE ao.executionPaper = ep)
          AND (:search = '' OR ep.executionPaperUniqueCode LIKE CONCAT('%', :search, '%')
            OR tp.tin LIKE CONCAT('%', :search, '%') OR np.nationalCode LIKE CONCAT('%', :search, '%')
            OR lp.nationalId LIKE CONCAT('%', :search, '%')
            OR CONCAT(ni.firstName, ' ', ni.lastName) LIKE CONCAT('%', :search, '%')
            OR li.name LIKE CONCAT('%', :search, '%'))
        """)
    Page<ExecutionPaper> findCandidates(@Param("national") boolean national, @Param("directorateId") Long directorateId,
            @Param("deadlineCutoff") LocalDateTime deadlineCutoff, @Param("search") String search, Pageable pageable);
}
