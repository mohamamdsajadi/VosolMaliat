package com.mparsict.vosolmaliat.application.dto.response;

import java.time.LocalDateTime;

public record ExecutionPaperTraceItemResponse(
        String type,
        String action,
        String label,
        String actorUsername,
        String oldValue,
        String newValue,
        LocalDateTime occurredAt
) {}
