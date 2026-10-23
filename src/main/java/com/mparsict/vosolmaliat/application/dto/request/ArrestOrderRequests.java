package com.mparsict.vosolmaliat.application.dto.request;

import jakarta.validation.constraints.*;
import java.util.List;

public final class ArrestOrderRequests {
    private ArrestOrderRequests() {}
    public enum Destination { USER, OFFICER, SPECIAL_DEPARTMENT }
    public record Start(@NotEmpty @Size(max = 100) List<@NotNull @Positive Long> executionPaperIds,
                        @NotNull Destination destination, Long supervisorId, Long officerId, @Size(max = 1000) String note) {}
    public record Transition(@NotNull @PositiveOrZero Long version, Long userId, @Size(max = 1000) String note) {}
}
