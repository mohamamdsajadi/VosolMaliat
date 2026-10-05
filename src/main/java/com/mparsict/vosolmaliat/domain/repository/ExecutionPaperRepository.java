package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExecutionPaperRepository extends JpaRepository<ExecutionPaper, Long> {

    Optional<ExecutionPaper> findByExecutionPaperUniqueCode(String code);

    Optional<ExecutionPaper> findByFinalTaxDebtPaperUniqueFinalTaxDebtNoticeId(String uniqueFinalTaxDebtNoticeId);



    /**
     * تاریخ صدور برگ بدهی متصل به برگ اجرا را برای بازیابی متغیر فرایند برمی‌گرداند.
     */
    @Query("""
            SELECT cast(finalTaxDebtPaper.finalTaxDebtNoticeIssueDate as string)
            FROM ExecutionPaper executionPaper
            JOIN executionPaper.finalTaxDebtPaper finalTaxDebtPaper
            WHERE executionPaper.id = :executionPaperId
            """)
    Optional<String> findDebtPaperIssueDateByExecutionPaperId(
            @Param("executionPaperId") Long executionPaperId);

    @Query("SELECT ep FROM ExecutionPaper ep WHERE ep.workflowState.currentState = :state")
    List<ExecutionPaper> findByCurrentState(@Param("state") String state);

    @Query("SELECT COUNT(ep) FROM ExecutionPaper ep WHERE ep.workflowState.currentState = :state")
    long countByCurrentState(@Param("state") String state);

    @Query("SELECT COUNT(ep) FROM ExecutionPaper ep WHERE ep.workflowState.currentState NOT IN :states")
    long countByCurrentStateNotIn(@Param("states") Collection<String> states);

    @Query("""
            SELECT COUNT(DISTINCT ep)
            FROM ExecutionPaper ep
            LEFT JOIN ExecutionPaperNotification notification ON notification.executionPaper = ep
            WHERE notification.notificationDate IS NOT NULL
            """)
    long countNotified();

    @Query("""
            SELECT COUNT(DISTINCT ep)
            FROM ExecutionPaper ep
            LEFT JOIN ExecutionPaperNotification notification ON notification.executionPaper = ep
            WHERE notification.notificationDate IS NULL
            """)
    long countNotNotified();

    @Query("SELECT ep FROM ExecutionPaper ep WHERE ep.workflowState.currentState = :state")
    Page<ExecutionPaper> findByCurrentState(@Param("state") String state, Pageable pageable);

    @Query("SELECT ep FROM ExecutionPaper ep WHERE ep.workflowState.currentState <> :state")
    Page<ExecutionPaper> findByCurrentStateNot(@Param("state") String state, Pageable pageable);

    @EntityGraph(attributePaths = {"finalTaxDebtPaper", "finalTaxDebtPaper.taxpayer"})
    @Query(value = """
            SELECT DISTINCT ep
            FROM ExecutionPaper ep
            LEFT JOIN ep.finalTaxDebtPaper finalTaxDebtPaper
            LEFT JOIN finalTaxDebtPaper.taxpayer taxpayer
            LEFT JOIN taxpayer.naturalPerson np
            LEFT JOIN taxpayer.legalPerson lp
            LEFT JOIN np.extraInfo ninfo
            LEFT JOIN lp.extraInfo linfo
            LEFT JOIN DebtInformation debt ON debt.executionPaper = ep
            LEFT JOIN ExecutionPaperNotification notification ON notification.executionPaper = ep
            WHERE (:assignmentMode IS NULL
                OR (:assignmentMode = 'ASSIGNED' AND ep.workflowState.currentState <> :unassignedState)
                OR (:assignmentMode = 'UNASSIGNED' AND ep.workflowState.currentState = :unassignedState))
              AND (:uniqueCode IS NULL OR LOWER(ep.executionPaperUniqueCode) LIKE LOWER(CONCAT('%', :uniqueCode, '%')))
              AND (:taxpayerName IS NULL
                OR LOWER(ninfo.firstName) LIKE LOWER(CONCAT('%', :taxpayerName, '%'))
                OR LOWER(ninfo.lastName) LIKE LOWER(CONCAT('%', :taxpayerName, '%'))
                OR LOWER(CONCAT(ninfo.firstName, ' ', ninfo.lastName)) LIKE LOWER(CONCAT('%', :taxpayerName, '%'))
                OR LOWER(linfo.name) LIKE LOWER(CONCAT('%', :taxpayerName, '%')))
              AND (:taxpayerNationalId IS NULL
                OR np.nationalCode LIKE CONCAT('%', :taxpayerNationalId, '%')
                OR lp.nationalId LIKE CONCAT('%', :taxpayerNationalId, '%'))
              AND (:currentState IS NULL OR ep.workflowState.currentState = :currentState)
              AND (:notificationStatus IS NULL
                OR (:notificationStatus = 'NOTIFIED' AND notification.notificationDate IS NOT NULL)
                OR (:notificationStatus = 'NOT_NOTIFIED' AND notification.notificationDate IS NULL))
            """,
            countQuery = """
            SELECT COUNT(DISTINCT ep)
            FROM ExecutionPaper ep
            LEFT JOIN ep.finalTaxDebtPaper finalTaxDebtPaper
            LEFT JOIN finalTaxDebtPaper.taxpayer taxpayer
            LEFT JOIN taxpayer.naturalPerson np
            LEFT JOIN taxpayer.legalPerson lp
            LEFT JOIN np.extraInfo ninfo
            LEFT JOIN lp.extraInfo linfo
            LEFT JOIN DebtInformation debt ON debt.executionPaper = ep
            LEFT JOIN ExecutionPaperNotification notification ON notification.executionPaper = ep
            WHERE (:assignmentMode IS NULL
                OR (:assignmentMode = 'ASSIGNED' AND ep.workflowState.currentState <> :unassignedState)
                OR (:assignmentMode = 'UNASSIGNED' AND ep.workflowState.currentState = :unassignedState))
              AND (:uniqueCode IS NULL OR LOWER(ep.executionPaperUniqueCode) LIKE LOWER(CONCAT('%', :uniqueCode, '%')))
              AND (:taxpayerName IS NULL
                OR LOWER(ninfo.firstName) LIKE LOWER(CONCAT('%', :taxpayerName, '%'))
                OR LOWER(ninfo.lastName) LIKE LOWER(CONCAT('%', :taxpayerName, '%'))
                OR LOWER(CONCAT(ninfo.firstName, ' ', ninfo.lastName)) LIKE LOWER(CONCAT('%', :taxpayerName, '%'))
                OR LOWER(linfo.name) LIKE LOWER(CONCAT('%', :taxpayerName, '%')))
              AND (:taxpayerNationalId IS NULL
                OR np.nationalCode LIKE CONCAT('%', :taxpayerNationalId, '%')
                OR lp.nationalId LIKE CONCAT('%', :taxpayerNationalId, '%'))
              AND (:currentState IS NULL OR ep.workflowState.currentState = :currentState)
              AND (:notificationStatus IS NULL
                OR (:notificationStatus = 'NOTIFIED' AND notification.notificationDate IS NOT NULL)
                OR (:notificationStatus = 'NOT_NOTIFIED' AND notification.notificationDate IS NULL))
            """)
    Page<ExecutionPaper> search(
            @Param("assignmentMode") String assignmentMode,
            @Param("unassignedState") String unassignedState,
            @Param("uniqueCode") String uniqueCode,
            @Param("taxpayerName") String taxpayerName,
            @Param("taxpayerNationalId") String taxpayerNationalId,
            @Param("currentState") String currentState,
            @Param("notificationStatus") String notificationStatus,
            Pageable pageable);

    @Query("SELECT ep FROM ExecutionPaper ep WHERE ep.workflowState.currentState = :state")
    List<ExecutionPaper> findAllByState(@Param("state") String state);
}
