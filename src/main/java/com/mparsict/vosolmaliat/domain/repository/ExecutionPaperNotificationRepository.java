package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaperNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExecutionPaperNotificationRepository extends JpaRepository<ExecutionPaperNotification, Long> {

    @Query("SELECT n FROM ExecutionPaperNotification n WHERE n.executionPaper.id = :executionPaperId")
    Optional<ExecutionPaperNotification> findByExecutionPaperId(@Param("executionPaperId") Long executionPaperId);
}
