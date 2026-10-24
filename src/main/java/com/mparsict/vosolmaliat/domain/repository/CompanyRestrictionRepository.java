package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.inquiry.RestrictionsAppliedToCompanies;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CompanyRestrictionRepository extends JpaRepository<RestrictionsAppliedToCompanies, Long> {
    @Query("select r from RestrictionsAppliedToCompanies r where r.executionPaper.id = :paperId order by r.id")
    List<RestrictionsAppliedToCompanies> findByExecutionPaperIdOrderByIdAsc(@Param("paperId") Long executionPaperId);
}
