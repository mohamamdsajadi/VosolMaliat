package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.rule.ExecutionPaperRejectionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExecutionPaperRejectionRuleRepository extends JpaRepository<ExecutionPaperRejectionRule, Long> {

    @Query("SELECT r FROM ExecutionPaperRejectionRule r WHERE (r.active = true OR r.active IS NULL) AND r.startDate <= :today AND r.endDate >= :today")
    List<ExecutionPaperRejectionRule> findActiveRules(@Param("today") String today);
}
