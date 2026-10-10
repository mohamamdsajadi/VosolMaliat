package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.integration.DebtPaperPushRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DebtPaperPushRecordRepository extends JpaRepository<DebtPaperPushRecord, Long> {
    Optional<DebtPaperPushRecord> findByIssuanceRequestId(Long issuanceRequestId);
    Optional<DebtPaperPushRecord> findByReferenceId(String referenceId);

    Optional<DebtPaperPushRecord> findFirstByExecutionPaper_IdOrderByIdDesc(Long executionPaperId);
}
