package com.mparsict.vosolmaliat.application.dto.request.security;

import jakarta.validation.constraints.NotBlank;

public record AssignGroupPermissionRequest(
        @NotBlank String groupName,
        @NotBlank String permissionCode
) {}
