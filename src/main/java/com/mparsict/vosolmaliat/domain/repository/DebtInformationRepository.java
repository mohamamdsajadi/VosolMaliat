package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.execution.DebtInformation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DebtInformationRepository extends JpaRepository<DebtInformation, Long> {

    @Query("SELECT d FROM DebtInformation d WHERE d.executionPaper.id = :executionPaperId")
    Optional<DebtInformation> findByExecutionPaperId(@Param("executionPaperId") Long executionPaperId);

    @Query("SELECT SUM(d.remainingTax) FROM DebtInformation d " +
           "JOIN d.executionPaper ep " +
           "WHERE ep.currentState = :state")
    Double sumRemainingTaxByState(@Param("state") String state);
}
