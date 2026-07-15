package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.rule.ExecutionPaperIssuanceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExecutionPaperIssuanceRequestRepository extends JpaRepository<ExecutionPaperIssuanceRequest, Long> {

    Optional<ExecutionPaperIssuanceRequest> findByHash(String hash);

    Optional<ExecutionPaperIssuanceRequest> findByDebtPaperUniqueCode(String code);

    long countByRejectionReasonIsNotNull();
}
