package com.mparsict.vosolmaliat.domain.repository.security;

import com.mparsict.vosolmaliat.domain.model.security.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    @org.springframework.data.jpa.repository.Query("""
        select count(distinct a.targetId) from AuditLog a where a.actorKeycloakUserId = :subject
          and a.targetType = 'CAMUNDA_USER_TASK'
          and a.action in ('TASK_COMPLETED', 'TASK_ASSIGNED', 'TASK_DELEGATED_TO_USER', 'TASK_DELEGATED_TO_GROUP')
        """)
    long countActedTasks(@org.springframework.data.repository.query.Param("subject") String subject);
    List<AuditLog> findByTargetTypeAndTargetIdOrderByCreatedAtAsc(String targetType, String targetId);
}
