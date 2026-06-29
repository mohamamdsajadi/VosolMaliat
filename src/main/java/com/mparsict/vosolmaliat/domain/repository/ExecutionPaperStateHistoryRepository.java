package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaperStateHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExecutionPaperStateHistoryRepository extends JpaRepository<ExecutionPaperStateHistory, Long> {

    List<ExecutionPaperStateHistory> findByExecutionPaperId(Long executionPaperId);
}
