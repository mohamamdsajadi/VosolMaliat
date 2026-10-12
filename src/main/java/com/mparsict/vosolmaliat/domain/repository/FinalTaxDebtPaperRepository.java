package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.execution.FinalTaxDebtPaper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@Repository
public interface FinalTaxDebtPaperRepository extends JpaRepository<FinalTaxDebtPaper, Long> {

    @Query("SELECT paper FROM FinalTaxDebtPaper paper WHERE paper.uniqueFinalTaxDebtNoticeId = :number OR paper.taxReturnNumber = :number")
    Optional<FinalTaxDebtPaper> findBySourceDocumentNumber(@Param("number") String number);
}
