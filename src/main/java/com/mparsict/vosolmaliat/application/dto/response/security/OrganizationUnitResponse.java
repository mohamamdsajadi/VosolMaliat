package com.mparsict.vosolmaliat.application.dto.response.security;

public record OrganizationUnitResponse(
        Long id,
        String name,
        String code,
        String type,
        Long parentId,
        Boolean enabled
) {}
