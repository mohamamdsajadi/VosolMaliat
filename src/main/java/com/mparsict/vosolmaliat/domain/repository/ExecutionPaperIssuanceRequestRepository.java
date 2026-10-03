package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.rule.ExecutionPaperIssuanceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExecutionPaperIssuanceRequestRepository extends JpaRepository<ExecutionPaperIssuanceRequest, Long> {

    Optional<ExecutionPaperIssuanceRequest> findFirstByHashOrderByIdDesc(String hash);
    default Optional<ExecutionPaperIssuanceRequest> findByHash(String hash) { return findFirstByHashOrderByIdDesc(hash); }

    Optional<ExecutionPaperIssuanceRequest> findFirstByDebtPaperUniqueCodeOrderByIdDesc(String code);
    default Optional<ExecutionPaperIssuanceRequest> findByDebtPaperUniqueCode(String code) { return findFirstByDebtPaperUniqueCodeOrderByIdDesc(code); }

    long countByRejectionReasonIsNotNull();

    List<ExecutionPaperIssuanceRequest> findByRejectionReasonIsNotNull();
}
