package com.mparsict.vosolmaliat.application.dto.response.admin;

import java.util.List;

public record AdminDataSummaryResponse(
        List<?> finalizedDebtPapers,
        List<?> taxpayers,
        List<?> executionPapers
) {}
