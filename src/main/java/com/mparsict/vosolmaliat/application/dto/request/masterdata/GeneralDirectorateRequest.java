package com.mparsict.vosolmaliat.application.dto.request.masterdata;

import jakarta.validation.constraints.NotBlank;

public record GeneralDirectorateRequest(@NotBlank String name) {}
