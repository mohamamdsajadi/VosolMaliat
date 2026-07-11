package com.mparsict.vosolmaliat.domain.repository.security;

import com.mparsict.vosolmaliat.domain.model.security.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
