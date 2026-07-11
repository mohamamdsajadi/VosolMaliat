package com.mparsict.vosolmaliat.application.dto.request.cartable;

import jakarta.validation.constraints.NotBlank;

public record AssignTaskRequest(@NotBlank String assignee) {}
