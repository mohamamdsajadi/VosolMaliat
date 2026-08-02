package com.mparsict.vosolmaliat.application.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SanimAuditRandomRequest(@Min(1) @Max(100) int count) {}
