package com.mparsict.vosolmaliat.application.dto.response.cartable;

public record TaskVariableResponse(
        String name,
        Object value,
        Boolean truncated
) {}
