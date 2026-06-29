package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.execution.ExecutionPaper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExecutionPaperRepository extends JpaRepository<ExecutionPaper, Long> {

    Optional<ExecutionPaper> findByExecutionPaperUniqueCode(String code);

    List<ExecutionPaper> findByCurrentState(String state);

    long countByCurrentState(String state);

    Page<ExecutionPaper> findByCurrentState(String state, Pageable pageable);

    Page<ExecutionPaper> findByCurrentStateNot(String state, Pageable pageable);

    @Query("SELECT ep FROM ExecutionPaper ep WHERE ep.currentState = :state")
    List<ExecutionPaper> findAllByState(@Param("state") String state);
}
