package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.rule.ExecutionPaperInformRejectionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExecutionPaperInformRejectionRuleRepository extends JpaRepository<ExecutionPaperInformRejectionRule, Long> {

    @Query("SELECT r FROM ExecutionPaperInformRejectionRule r WHERE r.startDate <= :today AND r.endDate >= :today")
    List<ExecutionPaperInformRejectionRule> findActiveRules(@Param("today") String today);
}
