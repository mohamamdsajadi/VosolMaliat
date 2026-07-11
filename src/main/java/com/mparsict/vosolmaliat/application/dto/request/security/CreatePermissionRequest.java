package com.mparsict.vosolmaliat.application.dto.request.security;

import jakarta.validation.constraints.NotBlank;

public record CreatePermissionRequest(
        @NotBlank String code,
        String description
) {}
