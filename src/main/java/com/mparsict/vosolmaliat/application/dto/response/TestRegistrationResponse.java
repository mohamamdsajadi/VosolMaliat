package com.mparsict.vosolmaliat.application.dto.response;

import java.time.OffsetDateTime;

public record TestRegistrationResponse(
        boolean saved,
        String message,
        OffsetDateTime registeredAt
) {
}
