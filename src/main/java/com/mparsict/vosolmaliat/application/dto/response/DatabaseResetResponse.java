package com.mparsict.vosolmaliat.application.dto.response;

import java.time.LocalDateTime;

public record DatabaseResetResponse(
        int tablesCleared,
        long rowsDeleted,
        LocalDateTime resetAt
) {}
