package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.execution.ArrestOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ArrestOrderRepository extends JpaRepository<ArrestOrder, Long>, JpaSpecificationExecutor<ArrestOrder> {
    boolean existsByExecutionPaperId(Long executionPaperId);

    @org.springframework.data.jpa.repository.Query("""
        select o.executionPaper.id from ArrestOrder o
        left join o.supervisor supervisor left join o.officer officer
        where (:supervisor = true and supervisor.keycloakUserId = :subject and o.issuedAt is not null)
           or (:supervisor = false and officer.keycloakUserId = :subject
               and o.stage = com.mparsict.vosolmaliat.domain.enums.ArrestOrderStage.ASSIGNED_TO_OFFICER)
        """)
    java.util.List<Long> findAssignedPaperIds(@org.springframework.data.repository.query.Param("subject") String subject,
                                            @org.springframework.data.repository.query.Param("supervisor") boolean supervisor);
}
