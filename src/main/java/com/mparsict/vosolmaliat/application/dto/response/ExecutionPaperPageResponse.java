package com.mparsict.vosolmaliat.application.dto.response;

import java.util.List;

public record ExecutionPaperPageResponse(
        List<ExecutionPaperDto> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages
) {}
