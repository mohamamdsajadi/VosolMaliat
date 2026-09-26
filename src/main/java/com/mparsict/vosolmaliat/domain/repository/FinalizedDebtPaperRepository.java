package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.execution.FinalizedDebtPaper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FinalizedDebtPaperRepository extends JpaRepository<FinalizedDebtPaper, Long> {

    Optional<FinalizedDebtPaper> findByDebtPaperUniqueCode(String code);
}
