package com.mparsict.vosolmaliat.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record DatabaseResetRequest(
        @NotNull
        @Pattern(regexp = "RESET_DATABASE", message = "confirmation must be RESET_DATABASE")
        String confirmation
) {}
