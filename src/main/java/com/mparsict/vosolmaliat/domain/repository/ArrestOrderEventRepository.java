package com.mparsict.vosolmaliat.domain.repository;

import com.mparsict.vosolmaliat.domain.model.execution.ArrestOrderEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ArrestOrderEventRepository extends JpaRepository<ArrestOrderEvent, Long> {
    List<ArrestOrderEvent> findByArrestOrderIdOrderByIdAsc(Long arrestOrderId);

    @org.springframework.data.jpa.repository.Query("""
        select distinct e.arrestOrder.id from ArrestOrderEvent e
        left join e.creatorUser actor
        where (actor.keycloakUserId = :subject or e.actorUsername = :username)
          and e.action in ('STARTED', 'SUPERVISOR_SELECTED', 'OFFICER_SELECTED',
                           'APPROVED_AND_ISSUED', 'ASSIGNED_TO_OFFICER', 'DRAFT_REFRESHED')
        """)
    List<Long> findActedOrderIds(@org.springframework.data.repository.query.Param("subject") String subject,
                                @org.springframework.data.repository.query.Param("username") String username);
}
