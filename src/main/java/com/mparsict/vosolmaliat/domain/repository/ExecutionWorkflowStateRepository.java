package com.mparsict.vosolmaliat.domain.repository;
import com.mparsict.vosolmaliat.domain.model.execution.ExecutionWorkflowState;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ExecutionWorkflowStateRepository extends JpaRepository<ExecutionWorkflowState, Long> {
    java.util.List<ExecutionWorkflowState> findTop100ByViewDateMessagePendingTrueOrderByIdAsc();
}
