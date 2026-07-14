package com.mparsict.vosolmaliat.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FakeElectronicNotificationCallbackRequest(
        @NotNull Long executionPaperId,
        @NotBlank String InformDate
) {}
