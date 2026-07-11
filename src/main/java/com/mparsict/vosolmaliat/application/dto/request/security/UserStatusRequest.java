package com.mparsict.vosolmaliat.application.dto.request.security;

import jakarta.validation.constraints.NotNull;

public record UserStatusRequest(@NotNull Boolean enabled) {}
