package com.mparsict.vosolmaliat.application.dto.response.security;

public record GroupPermissionResponse(
        Long id,
        String groupName,
        PermissionResponse permission
) {}
