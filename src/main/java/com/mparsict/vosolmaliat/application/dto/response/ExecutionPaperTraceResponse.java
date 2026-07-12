package com.mparsict.vosolmaliat.application.dto.response;

import java.util.List;

public record ExecutionPaperTraceResponse(
        Long executionPaperId,
        String executionPaperUniqueCode,
        List<ExecutionPaperTraceItemResponse> items
) {}
