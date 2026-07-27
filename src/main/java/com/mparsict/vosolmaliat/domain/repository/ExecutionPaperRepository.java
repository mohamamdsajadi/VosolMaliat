package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExecutionPaperRepository extends JpaRepository<ExecutionPaper, Long> {

    Optional<ExecutionPaper> findByExecutionPaperUniqueCode(String code);

    Optional<ExecutionPaper> findByFinalizedDebtPaperDebtPaperUniqueCode(String debtPaperUniqueCode);

    List<ExecutionPaper> findByCurrentState(String state);

    long countByCurrentState(String state);

    long countByCurrentStateNotIn(Collection<String> states);

    @Query("""
            SELECT COUNT(DISTINCT ep)
            FROM ExecutionPaper ep
            LEFT JOIN ExecutionPaperNotification notification ON notification.executionPaper = ep
            WHERE notification.electronicNotificationDate IS NOT NULL
            """)
    long countNotified();

    @Query("""
            SELECT COUNT(DISTINCT ep)
            FROM ExecutionPaper ep
            LEFT JOIN ExecutionPaperNotification notification ON notification.executionPaper = ep
            WHERE notification.electronicNotificationDate IS NULL
            """)
    long countNotNotified();

    Page<ExecutionPaper> findByCurrentState(String state, Pageable pageable);

    Page<ExecutionPaper> findByCurrentStateNot(String state, Pageable pageable);

    @Query(value = """
            SELECT DISTINCT ep
            FROM ExecutionPaper ep
            LEFT JOIN ep.taxpayer taxpayer
            LEFT JOIN taxpayer.naturalPerson np
            LEFT JOIN taxpayer.legalPerson lp
            LEFT JOIN DebtInformation debt ON debt.executionPaper = ep
            LEFT JOIN ExecutionPaperNotification notification ON notification.executionPaper = ep
            WHERE (:assignmentMode IS NULL
                OR (:assignmentMode = 'ASSIGNED' AND ep.currentState <> :unassignedState)
                OR (:assignmentMode = 'UNASSIGNED' AND ep.currentState = :unassignedState))
              AND (:uniqueCode IS NULL OR LOWER(ep.executionPaperUniqueCode) LIKE LOWER(CONCAT('%', :uniqueCode, '%')))
              AND (:taxpayerName IS NULL
                OR LOWER(np.firstName) LIKE LOWER(CONCAT('%', :taxpayerName, '%'))
                OR LOWER(np.lastName) LIKE LOWER(CONCAT('%', :taxpayerName, '%'))
                OR LOWER(CONCAT(np.firstName, ' ', np.lastName)) LIKE LOWER(CONCAT('%', :taxpayerName, '%'))
                OR LOWER(lp.name) LIKE LOWER(CONCAT('%', :taxpayerName, '%')))
              AND (:taxpayerNationalId IS NULL
                OR np.nationalCode LIKE CONCAT('%', :taxpayerNationalId, '%')
                OR lp.nationalId LIKE CONCAT('%', :taxpayerNationalId, '%'))
              AND (:currentState IS NULL OR ep.currentState = :currentState)
              AND (:taxUnitId IS NULL OR ep.taxUnit.id = :taxUnitId)
              AND (:notificationStatus IS NULL
                OR (:notificationStatus = 'NOTIFIED' AND notification.electronicNotificationDate IS NOT NULL)
                OR (:notificationStatus = 'NOT_NOTIFIED' AND notification.electronicNotificationDate IS NULL))
              AND (:minDebt IS NULL OR (
                    COALESCE(debt.taxAmount, 0)
                  + COALESCE(debt.dutyAmount, 0)
                  + COALESCE(debt.penaltyArticle22, 0)
                  + COALESCE(debt.penaltyArticle23, 0)
                  - COALESCE(debt.taxPaidAmount, 0)
                  - COALESCE(debt.penaltyPaidAmount, 0)
              ) >= :minDebt)
              AND (:maxDebt IS NULL OR (
                    COALESCE(debt.taxAmount, 0)
                  + COALESCE(debt.dutyAmount, 0)
                  + COALESCE(debt.penaltyArticle22, 0)
                  + COALESCE(debt.penaltyArticle23, 0)
                  - COALESCE(debt.taxPaidAmount, 0)
                  - COALESCE(debt.penaltyPaidAmount, 0)
              ) <= :maxDebt)
            """,
            countQuery = """
            SELECT COUNT(DISTINCT ep)
            FROM ExecutionPaper ep
            LEFT JOIN ep.taxpayer taxpayer
            LEFT JOIN taxpayer.naturalPerson np
            LEFT JOIN taxpayer.legalPerson lp
            LEFT JOIN DebtInformation debt ON debt.executionPaper = ep
            LEFT JOIN ExecutionPaperNotification notification ON notification.executionPaper = ep
            WHERE (:assignmentMode IS NULL
                OR (:assignmentMode = 'ASSIGNED' AND ep.currentState <> :unassignedState)
                OR (:assignmentMode = 'UNASSIGNED' AND ep.currentState = :unassignedState))
              AND (:uniqueCode IS NULL OR LOWER(ep.executionPaperUniqueCode) LIKE LOWER(CONCAT('%', :uniqueCode, '%')))
              AND (:taxpayerName IS NULL
                OR LOWER(np.firstName) LIKE LOWER(CONCAT('%', :taxpayerName, '%'))
                OR LOWER(np.lastName) LIKE LOWER(CONCAT('%', :taxpayerName, '%'))
                OR LOWER(CONCAT(np.firstName, ' ', np.lastName)) LIKE LOWER(CONCAT('%', :taxpayerName, '%'))
                OR LOWER(lp.name) LIKE LOWER(CONCAT('%', :taxpayerName, '%')))
              AND (:taxpayerNationalId IS NULL
                OR np.nationalCode LIKE CONCAT('%', :taxpayerNationalId, '%')
                OR lp.nationalId LIKE CONCAT('%', :taxpayerNationalId, '%'))
              AND (:currentState IS NULL OR ep.currentState = :currentState)
              AND (:taxUnitId IS NULL OR ep.taxUnit.id = :taxUnitId)
              AND (:notificationStatus IS NULL
                OR (:notificationStatus = 'NOTIFIED' AND notification.electronicNotificationDate IS NOT NULL)
                OR (:notificationStatus = 'NOT_NOTIFIED' AND notification.electronicNotificationDate IS NULL))
              AND (:minDebt IS NULL OR (
                    COALESCE(debt.taxAmount, 0)
                  + COALESCE(debt.dutyAmount, 0)
                  + COALESCE(debt.penaltyArticle22, 0)
                  + COALESCE(debt.penaltyArticle23, 0)
                  - COALESCE(debt.taxPaidAmount, 0)
                  - COALESCE(debt.penaltyPaidAmount, 0)
              ) >= :minDebt)
              AND (:maxDebt IS NULL OR (
                    COALESCE(debt.taxAmount, 0)
                  + COALESCE(debt.dutyAmount, 0)
                  + COALESCE(debt.penaltyArticle22, 0)
                  + COALESCE(debt.penaltyArticle23, 0)
                  - COALESCE(debt.taxPaidAmount, 0)
                  - COALESCE(debt.penaltyPaidAmount, 0)
              ) <= :maxDebt)
            """)
    Page<ExecutionPaper> search(
            @Param("assignmentMode") String assignmentMode,
            @Param("unassignedState") String unassignedState,
            @Param("uniqueCode") String uniqueCode,
            @Param("taxpayerName") String taxpayerName,
            @Param("taxpayerNationalId") String taxpayerNationalId,
            @Param("currentState") String currentState,
            @Param("notificationStatus") String notificationStatus,
            @Param("taxUnitId") Long taxUnitId,
            @Param("minDebt") Double minDebt,
            @Param("maxDebt") Double maxDebt,
            Pageable pageable);

    @Query("SELECT ep FROM ExecutionPaper ep WHERE ep.currentState = :state")
    List<ExecutionPaper> findAllByState(@Param("state") String state);
}
