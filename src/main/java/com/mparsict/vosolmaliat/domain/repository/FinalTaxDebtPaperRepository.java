package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.execution.FinalTaxDebtPaper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FinalTaxDebtPaperRepository extends JpaRepository<FinalTaxDebtPaper, Long> {

    Optional<FinalTaxDebtPaper> findByUniqueFinalTaxDebtNoticeId(String id);
}
