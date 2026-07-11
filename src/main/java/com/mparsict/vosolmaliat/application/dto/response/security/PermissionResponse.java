package com.mparsict.vosolmaliat.application.dto.response.security;

public record PermissionResponse(
        Long id,
        String code,
        String description,
        Boolean enabled
) {}
