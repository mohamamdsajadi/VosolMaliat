package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaperNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExecutionPaperNotificationRepository extends JpaRepository<ExecutionPaperNotification, Long> {

    Optional<ExecutionPaperNotification> findByExecutionPaperId(Long executionPaperId);
}
