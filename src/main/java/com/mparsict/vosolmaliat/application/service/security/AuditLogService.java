package com.mparsict.vosolmaliat.application.service.security;

import com.mparsict.vosolmaliat.application.security.CurrentUserContext;
import com.mparsict.vosolmaliat.domain.model.security.AuditLog;
import com.mparsict.vosolmaliat.domain.repository.security.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void record(CurrentUserContext actor, String action, String targetType, String targetId, String oldValue, String newValue) {
        auditLogRepository.save(AuditLog.builder()
                .actorUsername(actor == null ? "system" : actor.username())
                .actorKeycloakUserId(actor == null ? null : actor.keycloakUserId())
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .oldValue(oldValue)
                .newValue(newValue)
                .build());
    }
}
