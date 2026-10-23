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

    @Query("""
            SELECT new com.mparsict.vosolmaliat.application.dto.response.ExecutionPaperLinkResponse(ep.id, ep.executionPaperUniqueCode)
            FROM ExecutionPaper ep
            WHERE ep.finalTaxDebtPaper.taxpayer.id = :taxpayerId AND ep.id <> :paperId
              AND ep.workflowState.currentState NOT IN :closedStates
            ORDER BY ep.id DESC
            """)
    List<com.mparsict.vosolmaliat.application.dto.response.ExecutionPaperLinkResponse> findOtherActivePapers(
            @Param("taxpayerId") Long taxpayerId, @Param("paperId") Long paperId,
            @Param("closedStates") Collection<String> closedStates);

    @Query("SELECT paper FROM ExecutionPaper paper WHERE paper.finalTaxDebtPaper.uniqueFinalTaxDebtNoticeId = :number OR paper.finalTaxDebtPaper.taxReturnNumber = :number")
    Optional<ExecutionPaper> findByFinalTaxDebtPaperSourceDocumentNumber(@Param("number") String number);



    /**
     * تاریخ صدور برگ بدهی متصل به برگ اجرا را برای بازیابی متغیر فرایند برمی‌گرداند.
     */
    @Query("""
            SELECT cast(COALESCE(finalTaxDebtPaper.taxReturnDate, finalTaxDebtPaper.finalTaxDebtNoticeIssueDate) as string)
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

    @EntityGraph(attributePaths = {"finalTaxDebtPaper", "finalTaxDebtPaper.taxpayer", "finalTaxDebtPaper.paperTypeDefinition"})
    @Query(value = """
            SELECT DISTINCT ep
            FROM ExecutionPaper ep
            LEFT JOIN ep.finalTaxDebtPaper finalTaxDebtPaper
            LEFT JOIN finalTaxDebtPaper.taxpayer taxpayer
            LEFT JOIN taxpayer.naturalPerson np
            LEFT JOIN taxpayer.legalPerson lp
            LEFT JOIN np.extraInfo ninfo
            LEFT JOIN lp.extraInfo linfo
            LEFT JOIN ExecutionPaperNotification notification ON notification.executionPaper = ep
            WHERE (:restrictAssignments = false OR ep.id IN :visiblePaperIds)
              AND (:assignmentMode IS NULL
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
              AND (:search IS NULL
                OR LOWER(ep.executionPaperUniqueCode) LIKE LOWER(CONCAT('%', :search, '%'))
                OR np.nationalCode LIKE CONCAT('%', :search, '%')
                OR lp.nationalId LIKE CONCAT('%', :search, '%')
                OR taxpayer.tin LIKE CONCAT('%', :search, '%')
                OR LOWER(CONCAT(ninfo.firstName, ' ', ninfo.lastName)) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(linfo.name) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:taxpayerType IS NULL OR (:taxpayerType = 'NATURAL' AND np IS NOT NULL)
                OR (:taxpayerType = 'LEGAL' AND lp IS NOT NULL))
              AND (:issuedFrom IS NULL OR ep.issuanceDate >= :issuedFrom)
              AND (:issuedBefore IS NULL OR ep.issuanceDate < :issuedBefore)
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
            LEFT JOIN ExecutionPaperNotification notification ON notification.executionPaper = ep
            WHERE (:restrictAssignments = false OR ep.id IN :visiblePaperIds)
              AND (:assignmentMode IS NULL
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
              AND (:search IS NULL
                OR LOWER(ep.executionPaperUniqueCode) LIKE LOWER(CONCAT('%', :search, '%'))
                OR np.nationalCode LIKE CONCAT('%', :search, '%')
                OR lp.nationalId LIKE CONCAT('%', :search, '%')
                OR taxpayer.tin LIKE CONCAT('%', :search, '%')
                OR LOWER(CONCAT(ninfo.firstName, ' ', ninfo.lastName)) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(linfo.name) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:taxpayerType IS NULL OR (:taxpayerType = 'NATURAL' AND np IS NOT NULL)
                OR (:taxpayerType = 'LEGAL' AND lp IS NOT NULL))
              AND (:issuedFrom IS NULL OR ep.issuanceDate >= :issuedFrom)
              AND (:issuedBefore IS NULL OR ep.issuanceDate < :issuedBefore)
            """)
    Page<ExecutionPaper> search(
            @Param("restrictAssignments") boolean restrictAssignments,
            @Param("visiblePaperIds") Collection<Long> visiblePaperIds,
            @Param("assignmentMode") String assignmentMode,
            @Param("unassignedState") String unassignedState,
            @Param("uniqueCode") String uniqueCode,
            @Param("taxpayerName") String taxpayerName,
            @Param("taxpayerNationalId") String taxpayerNationalId,
            @Param("currentState") String currentState,
            @Param("notificationStatus") String notificationStatus,
            @Param("search") String search,
            @Param("taxpayerType") String taxpayerType,
            @Param("issuedFrom") java.time.LocalDateTime issuedFrom,
            @Param("issuedBefore") java.time.LocalDateTime issuedBefore,
            Pageable pageable);

    @Query("SELECT ep FROM ExecutionPaper ep WHERE ep.workflowState.currentState = :state")
    List<ExecutionPaper> findAllByState(@Param("state") String state);
}
