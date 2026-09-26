package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaperStateHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExecutionPaperStateHistoryRepository extends JpaRepository<ExecutionPaperStateHistory, Long> {

    @Query("SELECT h FROM ExecutionPaperStateHistory h WHERE h.executionPaper.id = :executionPaperId")
    List<ExecutionPaperStateHistory> findByExecutionPaperId(@Param("executionPaperId") Long executionPaperId);
}
