package com.mparsict.vosolmaliat.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DebtPaperPushData(
        @JsonProperty("executionPaperID") Long executionPaperId
) {
    public static DebtPaperPushData empty() {
        return new DebtPaperPushData(0L);
    }
}
