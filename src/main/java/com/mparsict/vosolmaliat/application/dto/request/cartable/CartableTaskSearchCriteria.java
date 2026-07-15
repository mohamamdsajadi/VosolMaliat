package com.mparsict.vosolmaliat.application.dto.request.cartable;

public record CartableTaskSearchCriteria(
        String executionPaper,
        String taskName,
        String assignee,
        String candidateGroup,
        String createdFrom,
        String createdTo
) {
    public static CartableTaskSearchCriteria empty() {
        return new CartableTaskSearchCriteria(null, null, null, null, null, null);
    }
}
